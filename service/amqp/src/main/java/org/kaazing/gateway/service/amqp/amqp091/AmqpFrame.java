/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kaazing.gateway.service.amqp.amqp091;


public enum AmqpFrame {
    METHOD((byte)1),
    HEADER((byte)2),
    BODY((byte)3),
    HEARTBEAT((byte)8);
    
    private final byte type;
    
    AmqpFrame(byte type) {
        this.type = type;
    }
    
    public byte type() {
        return type;
    }
    
    public static AmqpFrame get(byte kind) {
        switch (kind) {
            case 1:
                return AmqpFrame.METHOD;
            case 2:
                return AmqpFrame.HEADER;
            case 3:
                return AmqpFrame.BODY;
            //case 4: // possibly necessary due to inconsistent AMQP specification
            case 8:
                return AmqpFrame.HEARTBEAT;
            default:
                String s = "AmqpFrame(): " + 
                "Protocol violation - Invalid frame-type: " + kind;
                throw new IllegalStateException(s);
        }
    }

}
