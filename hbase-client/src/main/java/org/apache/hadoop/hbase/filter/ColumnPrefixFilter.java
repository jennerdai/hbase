/*
 *
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

package org.apache.hadoop.hbase.filter;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.exceptions.DeserializationException;
import org.apache.hadoop.hbase.protobuf.generated.FilterProtos;
import org.apache.hadoop.hbase.util.Bytes;

import java.util.ArrayList;

/**
 * This filter is used for selecting only those keys with columns that matches
 * a particular prefix. For example, if prefix is 'an', it will pass keys with
 * columns like 'and', 'anti' but not keys with columns like 'ball', 'act'.
 */
@InterfaceAudience.Public
@InterfaceStability.Stable
public class ColumnPrefixFilter extends FilterBase {
  protected byte [] prefix = null;

  public ColumnPrefixFilter(final byte [] prefix) {
    this.prefix = prefix;
  }

  public byte[] getPrefix() {
    return prefix;
  }

  @Override
  public ReturnCode filterKeyValue(KeyValue kv) {
    if (this.prefix == null || kv.getBuffer() == null) {
      return ReturnCode.INCLUDE;
    } else {
      return filterColumn(kv.getBuffer(), kv.getQualifierOffset(), kv.getQualifierLength());
    }
  }

  public ReturnCode filterColumn(byte[] buffer, int qualifierOffset, int qualifierLength) {
    if (qualifierLength < prefix.length) {
      int cmp = Bytes.compareTo(buffer, qualifierOffset, qualifierLength, this.prefix, 0,
          qualifierLength);
      if (cmp <= 0) {
        return ReturnCode.SEEK_NEXT_USING_HINT;
      } else {
        return ReturnCode.NEXT_ROW;
      }
    } else {
      int cmp = Bytes.compareTo(buffer, qualifierOffset, this.prefix.length, this.prefix, 0,
          this.prefix.length);
      if (cmp < 0) {
        return ReturnCode.SEEK_NEXT_USING_HINT;
      } else if (cmp > 0) {
        return ReturnCode.NEXT_ROW;
      } else {
        return ReturnCode.INCLUDE;
      }
    }
  }

  public static Filter createFilterFromArguments(ArrayList<byte []> filterArguments) {
    Preconditions.checkArgument(filterArguments.size() == 1,
                                "Expected 1 but got: %s", filterArguments.size());
    byte [] columnPrefix = ParseFilter.removeQuotesFromByteArray(filterArguments.get(0));
    return new ColumnPrefixFilter(columnPrefix);
  }

  /**
   * @return The filter serialized using pb
   */
  public byte [] toByteArray() {
    FilterProtos.ColumnPrefixFilter.Builder builder =
      FilterProtos.ColumnPrefixFilter.newBuilder();
    if (this.prefix != null) builder.setPrefix(ByteString.copyFrom(this.prefix));
    return builder.build().toByteArray();
  }

  /**
   * @param pbBytes A pb serialized {@link ColumnPrefixFilter} instance
   * @return An instance of {@link ColumnPrefixFilter} made from <code>bytes</code>
   * @throws org.apache.hadoop.hbase.exceptions.DeserializationException
   * @see #toByteArray
   */
  public static ColumnPrefixFilter parseFrom(final byte [] pbBytes)
  throws DeserializationException {
    FilterProtos.ColumnPrefixFilter proto;
    try {
      proto = FilterProtos.ColumnPrefixFilter.parseFrom(pbBytes);
    } catch (InvalidProtocolBufferException e) {
      throw new DeserializationException(e);
    }
    return new ColumnPrefixFilter(proto.getPrefix().toByteArray());
  }

  /**
   * @param other
   * @return true if and only if the fields of the filter that are serialized
   * are equal to the corresponding fields in other.  Used for testing.
   */
  boolean areSerializedFieldsEqual(Filter o) {
   if (o == this) return true;
   if (!(o instanceof ColumnPrefixFilter)) return false;

   ColumnPrefixFilter other = (ColumnPrefixFilter)o;
    return Bytes.equals(this.getPrefix(), other.getPrefix());
  }

  public KeyValue getNextKeyHint(KeyValue kv) {
    return KeyValue.createFirstOnRow(
        kv.getBuffer(), kv.getRowOffset(), kv.getRowLength(), kv.getBuffer(),
        kv.getFamilyOffset(), kv.getFamilyLength(), prefix, 0, prefix.length);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + " " + Bytes.toStringBinary(this.prefix);
  }
}
