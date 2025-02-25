/*
 * Copyright (c) 2010, 2011  StumbleUpon, Inc.  All rights reserved.
 * This file is part of Async HBase.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   - Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   - Neither the name of the StumbleUpon nor the names of its contributors
 *     may be used to endorse or promote products derived from this software
 *     without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.hbase.async;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * Puts some data into HBase.
 *
 * <h1>A note on passing {@code byte} arrays in argument</h1>
 * None of the method that receive a {@code byte[]} in argument will copy it.
 * For more info, please refer to the documentation of {@link HBaseRpc}.
 * <h1>A note on passing {@code String}s in argument</h1>
 * All strings are assumed to use the platform's default charset.
 */
public final class PutRequest extends HBaseRpc
  implements HBaseRpc.HasTable, HBaseRpc.HasKey, HBaseRpc.HasFamily,
             HBaseRpc.HasQualifier, HBaseRpc.HasValue {

  private static final byte[] PUT = new byte[] { 'p', 'u', 't' };

  /**
   * A put with all fields set to a 1-byte array containing a zero.
   * This is useful for loops that need to start with a valid-looking edit.
   */
  static final PutRequest EMPTY_PUT;
  static {
    final byte[] zero = new byte[] { 0 };
    EMPTY_PUT = new PutRequest(zero, zero, zero, zero, zero);
    EMPTY_PUT.setRegion(new RegionInfo(zero, zero, zero));
  }

  private final KeyValue kv;
  private final long lockid;
  private boolean durable = true;
  private boolean bufferable = true;

  /**
   * Constructor.
   * <strong>These byte arrays will NOT be copied.</strong>
   * @param table The table to edit.
   * @param key The key of the row to edit in that table.
   * @param family The column family to edit in that table.
   * @param qualifier The column qualifier to edit in that family.
   * @param value The value to store.
   */
  public PutRequest(final byte[] table,
                    final byte[] key,
                    final byte[] family,
                    final byte[] qualifier,
                    final byte[] value) {
    this(table, key, family, qualifier, value, RowLock.NO_LOCK);
  }

  /**
   * Constructor.
   * <strong>These byte arrays will NOT be copied.</strong>
   * @param table The table to edit.
   * @param key The key of the row to edit in that table.
   * @param family The column family to edit in that table.
   * @param qualifier The column qualifier to edit in that family.
   * @param value The value to store.
   * @param lock An explicit row lock to use with this request.
   */
  public PutRequest(final byte[] table,
                    final byte[] key,
                    final byte[] family,
                    final byte[] qualifier,
                    final byte[] value,
                    final RowLock lock) {
    this(table, key, family, qualifier, value, lock.id());
  }

  /**
   * Constructor.
   * @param table The table to edit.
   * @param key The key of the row to edit in that table.
   * @param family The column family to edit in that table.
   * @param qualifier The column qualifier to edit in that family.
   * @param value The value to store.
   */
  public PutRequest(final String table,
                    final String key,
                    final String family,
                    final String qualifier,
                    final String value) {
    this(table.getBytes(), key.getBytes(), family.getBytes(),
         qualifier.getBytes(), value.getBytes(), RowLock.NO_LOCK);
  }

  /**
   * Constructor.
   * @param table The table to edit.
   * @param key The key of the row to edit in that table.
   * @param family The column family to edit in that table.
   * @param qualifier The column qualifier to edit in that family.
   * @param value The value to store.
   * @param lock An explicit row lock to use with this request.
   */
  public PutRequest(final String table,
                    final String key,
                    final String family,
                    final String qualifier,
                    final String value,
                    final RowLock lock) {
    this(table.getBytes(), key.getBytes(), family.getBytes(),
         qualifier.getBytes(), value.getBytes(), lock.id());
  }

  /**
   * Constructor.
   * @param table The table to edit.
   * @param kv The {@link KeyValue} to store.
   * @since 1.1
   */
  public PutRequest(final byte[] table,
                    final KeyValue kv) {
    this(table, kv, RowLock.NO_LOCK);
  }

  /**
   * Constructor.
   * @param table The table to edit.
   * @param kv The {@link KeyValue} to store.
   * @param lock An explicit row lock to use with this request.
   * @since 1.1
   */
  public PutRequest(final byte[] table,
                    final KeyValue kv,
                    final RowLock lock) {
    this(table, kv, lock.id());
  }

  /** Private constructor.  */
  public PutRequest(final byte[] table,
                    final KeyValue kv,
                    final long lockid) {
    super(PUT, table, kv.key());
    this.kv = kv;
    this.lockid = lockid;
  }

  /** Private constructor.  */
  private PutRequest(final byte[] table,
                     final byte[] key,
                     final byte[] family,
                     final byte[] qualifier,
                     final byte[] value,
                     final long lockid) {
    super(PUT, table, key);
    // KeyValue's constructor will validate the remaining arguments.
    this.kv = new KeyValue(key, family, qualifier, Long.MAX_VALUE, value);
    this.lockid = lockid;
  }

  /**
   * Changes the durability setting of this edit.
   * The default is {@code true}.
   * Make sure you've read and understood the
   * <a href="HBaseClient.html#durability">data durability</a> section before
   * setting this to {@code false}.
   * @param durable Whether or not this edit should be stored with data
   * durability guarantee.
   */
  public void setDurable(final boolean durable) {
    this.durable = durable;
  }

  /**
   * Sets whether or not this edit is bufferable.
   * The default is {@code true}.
   * <p>
   * Setting this to {@code false} bypasses the client-based buffering and
   * causes this edit to be sent directly to the server.
   * @param bufferable Whether or not this edit can be buffered (i.e. delayed)
   * before being sent out to HBase.
   * @see HBaseClient#setFlushInterval
   */
  public void setBufferable(final boolean bufferable) {
    this.bufferable = bufferable;
  }

  @Override
  public byte[] table() {
    return table;
  }

  @Override
  public byte[] key() {
    return key;
  }

  @Override
  public byte[] family() {
    return kv.family();
  }

  @Override
  public byte[] qualifier() {
    return kv.qualifier();
  }

  @Override
  public byte[] value() {
    return kv.value();
  }

  public String toString() {
    return super.toStringWithQualifier("PutRequest",
                                       kv.family(), kv.qualifier(),
                                       ", value=" + Bytes.pretty(kv.value())
                                       + ", lockid=" + lockid
                                       + ", durable=" + durable
                                       + ", bufferable=" + bufferable);
  }

  // ---------------------- //
  // Package private stuff. //
  // ---------------------- //

  KeyValue kv() {
    return kv;
  }

  long lockid() {
    return lockid;
  }

  boolean durable() {
    return durable;
  }

  /** Returns whether or not it's OK to buffer this edit on the client side. */
  boolean canBuffer() {
    // Don't buffer edits that have a row-lock, we want those to
    // complete ASAP so as to not hold the lock for too long.
    return lockid == RowLock.NO_LOCK && bufferable;
  }

  /**
   * Predicts a lower bound on the serialized size of this RPC.
   * This is to avoid using a dynamic buffer, to avoid re-sizing the buffer.
   * Since we use a static buffer, if the prediction is wrong and turns out
   * to be less than what we need, there will be an exception which will
   * prevent the RPC from being serialized.  That'd be a severe bug.
   */
  private int predictSerializedSize() {
    int size = 0;
    size += 4;  // int:  Number of parameters.
    size += 1;  // byte: Type of the 1st parameter.
    size += 3;  // vint: region name length (3 bytes => max length = 32768).
    size += region.name().length;  // The region name.

    size += 1;  // byte: Type of the 2nd parameter.
    size += 1;  // byte: Type again (see HBASE-2877).

    size += 1;  // byte: Version of Put.
    size += 3;  // vint: row key length (3 bytes => max length = 32768).
    size += kv.key().length;  // The row key.
    size += 8;  // long: Timestamp.
    size += 8;  // long: Lock ID.
    size += 1;  // bool: Whether or not to write to the WAL.
    size += 4;  // int:  Number of families for which we have edits.

    size += 1;  // vint: Family length (guaranteed on 1 byte).
    size += kv.family().length;  // The family.
    size += 4;  // int:  Number of KeyValues that follow.
    size += 4;  // int:  Total number of bytes for all those KeyValues.

    size += kv.predictSerializedSize();

    return size;
  }

  /** Serializes this request.  */
  ChannelBuffer serialize(final byte unused_server_version) {
    final ChannelBuffer buf = newBuffer(predictSerializedSize());
    buf.writeInt(2);  // Number of parameters.

    // 1st param: byte array containing region name
    writeHBaseByteArray(buf, region.name());

    // 2nd param: Put object
    buf.writeByte(35);   // Code for a `Put' parameter.
    buf.writeByte(35);   // Code again (see HBASE-2877).
    buf.writeByte(1);    // Put#PUT_VERSION.  Undocumented versioning of Put.
    writeByteArray(buf, key);  // The row key.

    // Timestamp.  We always set it to Long.MAX_VALUE, which means "unset".
    // The RegionServer will set it for us, right before writing to the WAL
    // (or to the Memstore if we're not using the WAL).
    buf.writeLong(Long.MAX_VALUE);

    buf.writeLong(lockid);    // Lock ID.
    buf.writeByte(durable ? 0x01 : 0x00);  // Whether or not to use the WAL.

    buf.writeInt(1);  // Number of families that follow.
    writeByteArray(buf, kv.family());  // The column family.

    buf.writeInt(1);  // Number of "KeyValues" that follow.
    buf.writeInt(kv.predictSerializedSize());  // Size of the KV that follows.
    kv.serialize(buf, KeyValue.PUT);
    return buf;
  }

}
