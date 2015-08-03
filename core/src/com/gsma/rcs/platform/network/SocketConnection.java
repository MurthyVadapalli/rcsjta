/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.platform.network;

import com.gsma.rcs.core.ims.protocol.sip.SipPayloadException;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Socket client connection
 * 
 * @author jexa7410
 */
public interface SocketConnection extends Closeable {
    /**
     * Open the socket
     * 
     * @param remoteAddr Remote address
     * @param remotePort Remote port
     * @throws IOException
     * @throws SipPayloadException
     */
    public void open(String remoteAddr, int remotePort) throws IOException, SipPayloadException;

    /**
     * Close the socket
     * 
     * @throws IOException
     */
    public void close() throws IOException;

    /**
     * Returns the socket input stream
     * 
     * @return Input stream
     * @throws IOException
     */
    public InputStream getInputStream() throws IOException;

    /**
     * Returns the socket output stream
     * 
     * @return Output stream
     * @throws IOException
     */
    public OutputStream getOutputStream() throws IOException;

    /**
     * Returns the remote address of the connection
     * 
     * @return Address
     * @throws IOException
     */
    public String getRemoteAddress() throws IOException;

    /**
     * Returns the remote port of the connection
     * 
     * @return Port
     * @throws IOException
     */
    public int getRemotePort() throws IOException;

    /**
     * Returns the local address of the connection
     * 
     * @return Address
     * @throws IOException
     */
    public String getLocalAddress() throws IOException;

    /**
     * Returns the local port of the connection
     * 
     * @return Port
     * @throws IOException
     */
    public int getLocalPort() throws IOException;

    /**
     * Get the timeout for this socket during which a reading operation shall block while waiting
     * for data
     * 
     * @return Milliseconds
     * @throws IOException
     */
    public int getSoTimeout() throws IOException;

    /**
     * Set the timeout for this socket during which a reading operation shall block while waiting
     * for data
     * 
     * @param timeout Timeout in milliseconds
     * @throws IOException
     */
    public void setSoTimeout(long timeout) throws IOException;
}