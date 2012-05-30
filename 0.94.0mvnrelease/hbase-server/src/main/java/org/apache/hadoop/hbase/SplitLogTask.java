/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hbase;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.hbase.protobuf.ProtobufUtil;
import org.apache.hadoop.hbase.protobuf.generated.HBaseProtos;
import org.apache.hadoop.hbase.protobuf.generated.ZooKeeperProtos;
import org.apache.hadoop.hbase.util.Bytes;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * State of a WAL log split during distributed splitting.  State is kept up in zookeeper.
 * Encapsulates protobuf serialization/deserialization so we don't leak generated pb outside of
 * this class.  Used by regionserver and master packages.
 * <p>Immutable
 */
@InterfaceAudience.Private
public class SplitLogTask {
  private final ServerName originServer;
  private final ZooKeeperProtos.SplitLogTask.State state;

  public static class Unassigned extends SplitLogTask {
    public Unassigned(final ServerName originServer) {
      super(originServer, ZooKeeperProtos.SplitLogTask.State.UNASSIGNED);
    }
  }

  public static class Owned extends SplitLogTask {
    public Owned(final ServerName originServer) {
      super(originServer, ZooKeeperProtos.SplitLogTask.State.OWNED);
    }
  }

  public static class Resigned extends SplitLogTask {
    public Resigned(final ServerName originServer) {
      super(originServer, ZooKeeperProtos.SplitLogTask.State.RESIGNED);
    }
  }

  public static class Done extends SplitLogTask {
    public Done(final ServerName originServer) {
      super(originServer, ZooKeeperProtos.SplitLogTask.State.DONE);
    }
  }

  public static class Err extends SplitLogTask {
    public Err(final ServerName originServer) {
      super(originServer, ZooKeeperProtos.SplitLogTask.State.ERR);
    }
  }

  SplitLogTask(final ZooKeeperProtos.SplitLogTask slt) {
    this(ProtobufUtil.toServerName(slt.getServerName()), slt.getState());
  }

  SplitLogTask(final ServerName originServer, final ZooKeeperProtos.SplitLogTask.State state) {
    this.originServer = originServer;
    this.state = state;
  }

  public ServerName getServerName() {
    return this.originServer;
  }

  public boolean isUnassigned(final ServerName sn) {
    return this.originServer.equals(sn) && isUnassigned();
  }

  public boolean isUnassigned() {
    return this.state == ZooKeeperProtos.SplitLogTask.State.UNASSIGNED;
  }

  public boolean isOwned(final ServerName sn) {
    return this.originServer.equals(sn) && isOwned();
  }

  public boolean isOwned() {
    return this.state == ZooKeeperProtos.SplitLogTask.State.OWNED;
  }

  public boolean isResigned(final ServerName sn) {
    return this.originServer.equals(sn) && isResigned();
  }

  public boolean isResigned() {
    return this.state == ZooKeeperProtos.SplitLogTask.State.RESIGNED;
  }

  public boolean isDone(final ServerName sn) {
    return this.originServer.equals(sn) && isDone();
  }

  public boolean isDone() {
    return this.state == ZooKeeperProtos.SplitLogTask.State.DONE;
  }

  public boolean isErr(final ServerName sn) {
    return this.originServer.equals(sn) && isErr();
  }

  public boolean isErr() {
    return this.state == ZooKeeperProtos.SplitLogTask.State.ERR;
  }

  @Override
  public String toString() {
    return this.state.toString() + " " + this.originServer.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SplitLogTask)) return false;
    SplitLogTask other = (SplitLogTask)obj;
    return other.state.equals(this.state) && other.originServer.equals(this.originServer);
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 31 * hash + this.state.hashCode();
    return 31 * hash + this.originServer.hashCode();
  }

  /**
   * @param data Serialized date to parse.
   * @return An SplitLogTaskState instance made of the passed <code>data</code>
   * @throws DeserializationException 
   * @see #toByteArray()
   */
  public static SplitLogTask parseFrom(final byte [] data) throws DeserializationException {
    ProtobufUtil.expectPBMagicPrefix(data);
    try {
      int prefixLen = ProtobufUtil.lengthOfPBMagic();
      ZooKeeperProtos.SplitLogTask slt = ZooKeeperProtos.SplitLogTask.newBuilder().
        mergeFrom(data, prefixLen, data.length - prefixLen).build();
      return new SplitLogTask(slt);
    } catch (InvalidProtocolBufferException e) {
      throw new DeserializationException(Bytes.toStringBinary(data, 0, 64), e);
    }
  }

  /**
   * @return This instance serialized into a byte array
   * @see #parseFrom(byte[])
   */
  public byte [] toByteArray() {
    // First create a pb ServerName.  Then create a ByteString w/ the TaskState
    // bytes in it.  Finally create a SplitLogTaskState passing in the two
    // pbs just created.
    HBaseProtos.ServerName snpb = ProtobufUtil.toServerName(this.originServer);
    ZooKeeperProtos.SplitLogTask slts =
      ZooKeeperProtos.SplitLogTask.newBuilder().setServerName(snpb).setState(this.state).build();
    return ProtobufUtil.prependPBMagic(slts.toByteArray());
  }
}