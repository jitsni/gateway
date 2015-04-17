/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.service.http.proxy;

import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;

import static java.nio.charset.StandardCharsets.UTF_8;

class SecureOriginServer implements Runnable {
    private final KeyStore keyStore;
    private final char[] password;
    private final int port;
    private final Handler handler;
    private volatile boolean stopped;
    private SSLServerSocket socket;

    interface Handler {
        void handle(SSLSocket sslSocket);
    }

    SecureOriginServer(int port, KeyStore keyStore, char[] password, Handler handler) {
        this.port = port;
        this.keyStore = keyStore;
        this.password = password;
        this.handler = handler;
    }

    void start() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password);
        sslContext.init(kmf.getKeyManagers(), null, null);
        ServerSocketFactory serverSocketFactory = sslContext.getServerSocketFactory();

        socket = (SSLServerSocket) serverSocketFactory.createServerSocket(port);
        new Thread(this, "SSL Origin Server").start();
    }

    @Override
    public void run() {
        while (!stopped) {
            try {
                SSLSocket acceptSocket = (SSLSocket) socket.accept();
                try {
                    handler.handle(acceptSocket);
                } catch (Exception ioe) {
                    ioe.printStackTrace();
                }
            } catch (IOException ioe) {
                // no-op
            }
        }
    }

    void stop() throws IOException {
        stopped = true;
        if (socket != null) {
            socket.close();
        }
    }



    /*
     * A simple http server
     */
    static class HttpHandler implements Handler {
        final byte[] resBytes;

        enum State {
            START, SLASH_R, SLASH_RN, SLASH_RNR, END
        }

        HttpHandler(String res) {
            resBytes = res.getBytes(UTF_8);
        }

        @Override
        public void handle(SSLSocket acceptSocket) {
            try (SSLSocket socket = acceptSocket;
                 InputStream in = socket.getInputStream();
                 OutputStream out = socket.getOutputStream()) {

                parseHttpHeaders(in);
                out.write(resBytes);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        static void parseHttpHeaders(InputStream in) throws IOException {
            State state = State.START;
            while (state != State.END) {
                int i = in.read();
                switch (state) {
                    case START:
                        state = (i == '\r') ? State.SLASH_R : State.START;
                        break;
                    case SLASH_R:
                        state = (i == '\n') ? State.SLASH_RN : State.START;
                        break;
                    case SLASH_RN:
                        state = (i == '\r') ? State.SLASH_RNR : State.START;
                        break;
                    case SLASH_RNR:
                        state = (i == '\n') ? State.END : State.START;
                        break;
                }
            }
        }
    }


}