package com.eveningoutpost.dexdrip.cloud.nightlite;

@com.google.protobuf.Generated
public final class NightLiteProto extends com.google.protobuf.GeneratedFile {
  private NightLiteProto() {}
  static {
    com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(
      com.google.protobuf.RuntimeVersion.RuntimeDomain.PUBLIC,
      /* major= */ 4,
      /* minor= */ 34,
      /* patch= */ 0,
      /* suffix= */ "",
      "NightLiteProto");
  }
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  /**
   * Protobuf enum {@code Trend}
   */
  public enum Trend
      implements com.google.protobuf.ProtocolMessageEnum {
    /**
     * <pre>
     * proto3 requires 0 as the first value
     * </pre>
     *
     * <code>TREND_UNKNOWN = 0;</code>
     */
    TREND_UNKNOWN(0),
    /**
     * <code>NONE = 1;</code>
     */
    NONE(1),
    /**
     * <code>DOUBLE_UP = 2;</code>
     */
    DOUBLE_UP(2),
    /**
     * <code>SINGLE_UP = 3;</code>
     */
    SINGLE_UP(3),
    /**
     * <code>UP_45 = 4;</code>
     */
    UP_45(4),
    /**
     * <code>FLAT = 5;</code>
     */
    FLAT(5),
    /**
     * <code>DOWN_45 = 6;</code>
     */
    DOWN_45(6),
    /**
     * <code>SINGLE_DOWN = 7;</code>
     */
    SINGLE_DOWN(7),
    /**
     * <code>DOUBLE_DOWN = 8;</code>
     */
    DOUBLE_DOWN(8),
    /**
     * <code>NOT_COMPUTABLE = 9;</code>
     */
    NOT_COMPUTABLE(9),
    /**
     * <code>OUT_OF_RANGE = 10;</code>
     */
    OUT_OF_RANGE(10),
    UNRECOGNIZED(-1),
    ;

    static {
      com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(
        com.google.protobuf.RuntimeVersion.RuntimeDomain.PUBLIC,
        /* major= */ 4,
        /* minor= */ 34,
        /* patch= */ 0,
        /* suffix= */ "",
        "Trend");
    }
    /**
     * <pre>
     * proto3 requires 0 as the first value
     * </pre>
     *
     * <code>TREND_UNKNOWN = 0;</code>
     */
    public static final int TREND_UNKNOWN_VALUE = 0;
    /**
     * <code>NONE = 1;</code>
     */
    public static final int NONE_VALUE = 1;
    /**
     * <code>DOUBLE_UP = 2;</code>
     */
    public static final int DOUBLE_UP_VALUE = 2;
    /**
     * <code>SINGLE_UP = 3;</code>
     */
    public static final int SINGLE_UP_VALUE = 3;
    /**
     * <code>UP_45 = 4;</code>
     */
    public static final int UP_45_VALUE = 4;
    /**
     * <code>FLAT = 5;</code>
     */
    public static final int FLAT_VALUE = 5;
    /**
     * <code>DOWN_45 = 6;</code>
     */
    public static final int DOWN_45_VALUE = 6;
    /**
     * <code>SINGLE_DOWN = 7;</code>
     */
    public static final int SINGLE_DOWN_VALUE = 7;
    /**
     * <code>DOUBLE_DOWN = 8;</code>
     */
    public static final int DOUBLE_DOWN_VALUE = 8;
    /**
     * <code>NOT_COMPUTABLE = 9;</code>
     */
    public static final int NOT_COMPUTABLE_VALUE = 9;
    /**
     * <code>OUT_OF_RANGE = 10;</code>
     */
    public static final int OUT_OF_RANGE_VALUE = 10;


    public final int getNumber() {
      if (this == UNRECOGNIZED) {
        throw new java.lang.IllegalArgumentException(
            "Can't get the number of an unknown enum value.");
      }
      return value;
    }

    /**
     * @param value The numeric wire value of the corresponding enum entry.
     * @return The enum associated with the given numeric wire value.
     * @deprecated Use {@link #forNumber(int)} instead.
     */
    @java.lang.Deprecated
    public static Trend valueOf(int value) {
      return forNumber(value);
    }

    /**
     * @param value The numeric wire value of the corresponding enum entry.
     * @return The enum associated with the given numeric wire value.
     */
    public static Trend forNumber(int value) {
      switch (value) {
        case 0: return TREND_UNKNOWN;
        case 1: return NONE;
        case 2: return DOUBLE_UP;
        case 3: return SINGLE_UP;
        case 4: return UP_45;
        case 5: return FLAT;
        case 6: return DOWN_45;
        case 7: return SINGLE_DOWN;
        case 8: return DOUBLE_DOWN;
        case 9: return NOT_COMPUTABLE;
        case 10: return OUT_OF_RANGE;
        default: return null;
      }
    }

    public static com.google.protobuf.Internal.EnumLiteMap<Trend>
        internalGetValueMap() {
      return internalValueMap;
    }
    private static final com.google.protobuf.Internal.EnumLiteMap<
        Trend> internalValueMap =
          new com.google.protobuf.Internal.EnumLiteMap<Trend>() {
            public Trend findValueByNumber(int number) {
              return Trend.forNumber(number);
            }
          };

    public final com.google.protobuf.Descriptors.EnumValueDescriptor
        getValueDescriptor() {
      if (this == UNRECOGNIZED) {
        throw new java.lang.IllegalStateException(
            "Can't get the descriptor of an unrecognized enum value.");
      }
      return getDescriptor().getValue(ordinal());
    }
    public final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptorForType() {
      return getDescriptor();
    }
    public static com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptor() {
      return NightLiteProto.getDescriptor().getEnumType(0);
    }

    private static final Trend[] VALUES = values();

    public static Trend valueOf(
        com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
      if (desc.getType() != getDescriptor()) {
        throw new java.lang.IllegalArgumentException(
          "EnumValueDescriptor is not for this type.");
      }
      if (desc.getIndex() == -1) {
        return UNRECOGNIZED;
      }
      return VALUES[desc.getIndex()];
    }

    private final int value;

    private Trend(int value) {
      this.value = value;
    }

    // @@protoc_insertion_point(enum_scope:Trend)
  }

  /**
   * Protobuf enum {@code Units}
   */
  public enum Units
      implements com.google.protobuf.ProtocolMessageEnum {
    /**
     * <code>MGDL = 0;</code>
     */
    MGDL(0),
    /**
     * <code>MMOL = 1;</code>
     */
    MMOL(1),
    UNRECOGNIZED(-1),
    ;

    static {
      com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(
        com.google.protobuf.RuntimeVersion.RuntimeDomain.PUBLIC,
        /* major= */ 4,
        /* minor= */ 34,
        /* patch= */ 0,
        /* suffix= */ "",
        "Units");
    }
    /**
     * <code>MGDL = 0;</code>
     */
    public static final int MGDL_VALUE = 0;
    /**
     * <code>MMOL = 1;</code>
     */
    public static final int MMOL_VALUE = 1;


    public final int getNumber() {
      if (this == UNRECOGNIZED) {
        throw new java.lang.IllegalArgumentException(
            "Can't get the number of an unknown enum value.");
      }
      return value;
    }

    /**
     * @param value The numeric wire value of the corresponding enum entry.
     * @return The enum associated with the given numeric wire value.
     * @deprecated Use {@link #forNumber(int)} instead.
     */
    @java.lang.Deprecated
    public static Units valueOf(int value) {
      return forNumber(value);
    }

    /**
     * @param value The numeric wire value of the corresponding enum entry.
     * @return The enum associated with the given numeric wire value.
     */
    public static Units forNumber(int value) {
      switch (value) {
        case 0: return MGDL;
        case 1: return MMOL;
        default: return null;
      }
    }

    public static com.google.protobuf.Internal.EnumLiteMap<Units>
        internalGetValueMap() {
      return internalValueMap;
    }
    private static final com.google.protobuf.Internal.EnumLiteMap<
        Units> internalValueMap =
          new com.google.protobuf.Internal.EnumLiteMap<Units>() {
            public Units findValueByNumber(int number) {
              return Units.forNumber(number);
            }
          };

    public final com.google.protobuf.Descriptors.EnumValueDescriptor
        getValueDescriptor() {
      if (this == UNRECOGNIZED) {
        throw new java.lang.IllegalStateException(
            "Can't get the descriptor of an unrecognized enum value.");
      }
      return getDescriptor().getValue(ordinal());
    }
    public final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptorForType() {
      return getDescriptor();
    }
    public static com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptor() {
      return NightLiteProto.getDescriptor().getEnumType(1);
    }

    private static final Units[] VALUES = values();

    public static Units valueOf(
        com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
      if (desc.getType() != getDescriptor()) {
        throw new java.lang.IllegalArgumentException(
          "EnumValueDescriptor is not for this type.");
      }
      if (desc.getIndex() == -1) {
        return UNRECOGNIZED;
      }
      return VALUES[desc.getIndex()];
    }

    private final int value;

    private Units(int value) {
      this.value = value;
    }

    // @@protoc_insertion_point(enum_scope:Units)
  }

  public interface GlucoseEntryOrBuilder extends
      // @@protoc_insertion_point(interface_extends:GlucoseEntry)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <pre>
     * mgdl
     * </pre>
     *
     * <code>uint32 glucose = 1;</code>
     * @return The glucose.
     */
    int getGlucose();

    /**
     * <pre>
     * special epoch seconds
     * </pre>
     *
     * <code>uint32 timestamp = 2;</code>
     * @return The timestamp.
     */
    int getTimestamp();

    /**
     * <pre>
     * mgdl * 10
     * </pre>
     *
     * <code>int32 delta = 3;</code>
     * @return The delta.
     */
    int getDelta();

    /**
     * <code>.Trend trend = 4;</code>
     * @return The enum numeric value on the wire for trend.
     */
    int getTrendValue();
    /**
     * <code>.Trend trend = 4;</code>
     * @return The trend.
     */
    NightLiteProto.Trend getTrend();
  }
  /**
   * Protobuf type {@code GlucoseEntry}
   */
  public static final class GlucoseEntry extends
      com.google.protobuf.GeneratedMessage implements
      // @@protoc_insertion_point(message_implements:GlucoseEntry)
      GlucoseEntryOrBuilder {
  private static final long serialVersionUID = 0L;
    static {
      com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(
        com.google.protobuf.RuntimeVersion.RuntimeDomain.PUBLIC,
        /* major= */ 4,
        /* minor= */ 34,
        /* patch= */ 0,
        /* suffix= */ "",
        "GlucoseEntry");
    }
    // Use GlucoseEntry.newBuilder() to construct.
    private GlucoseEntry(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
      super(builder);
    }
    private GlucoseEntry() {
      trend_ = 0;
    }

    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return NightLiteProto.internal_static_GlucoseEntry_descriptor;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return NightLiteProto.internal_static_GlucoseEntry_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return NightLiteProto.internal_static_GlucoseEntry_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              NightLiteProto.GlucoseEntry.class, NightLiteProto.GlucoseEntry.Builder.class);
    }

    public static final int GLUCOSE_FIELD_NUMBER = 1;
    private int glucose_ = 0;
    /**
     * <pre>
     * mgdl
     * </pre>
     *
     * <code>uint32 glucose = 1;</code>
     * @return The glucose.
     */
    @java.lang.Override
    public int getGlucose() {
      return glucose_;
    }

    public static final int TIMESTAMP_FIELD_NUMBER = 2;
    private int timestamp_ = 0;
    /**
     * <pre>
     * special epoch seconds
     * </pre>
     *
     * <code>uint32 timestamp = 2;</code>
     * @return The timestamp.
     */
    @java.lang.Override
    public int getTimestamp() {
      return timestamp_;
    }

    public static final int DELTA_FIELD_NUMBER = 3;
    private int delta_ = 0;
    /**
     * <pre>
     * mgdl * 10
     * </pre>
     *
     * <code>int32 delta = 3;</code>
     * @return The delta.
     */
    @java.lang.Override
    public int getDelta() {
      return delta_;
    }

    public static final int TREND_FIELD_NUMBER = 4;
    private int trend_ = 0;
    /**
     * <code>.Trend trend = 4;</code>
     * @return The enum numeric value on the wire for trend.
     */
    @java.lang.Override public int getTrendValue() {
      return trend_;
    }
    /**
     * <code>.Trend trend = 4;</code>
     * @return The trend.
     */
    @java.lang.Override public NightLiteProto.Trend getTrend() {
      NightLiteProto.Trend result = NightLiteProto.Trend.forNumber(trend_);
      return result == null ? NightLiteProto.Trend.UNRECOGNIZED : result;
    }

    private byte memoizedIsInitialized = -1;
    @java.lang.Override
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      memoizedIsInitialized = 1;
      return true;
    }

    @java.lang.Override
    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      if (glucose_ != 0) {
        output.writeUInt32(1, glucose_);
      }
      if (timestamp_ != 0) {
        output.writeUInt32(2, timestamp_);
      }
      if (delta_ != 0) {
        output.writeInt32(3, delta_);
      }
      if (trend_ != NightLiteProto.Trend.TREND_UNKNOWN.getNumber()) {
        output.writeEnum(4, trend_);
      }
      getUnknownFields().writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      if (glucose_ != 0) {
        size += com.google.protobuf.CodedOutputStream
          .computeUInt32Size(1, glucose_);
      }
      if (timestamp_ != 0) {
        size += com.google.protobuf.CodedOutputStream
          .computeUInt32Size(2, timestamp_);
      }
      if (delta_ != 0) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt32Size(3, delta_);
      }
      if (trend_ != NightLiteProto.Trend.TREND_UNKNOWN.getNumber()) {
        size += com.google.protobuf.CodedOutputStream
          .computeEnumSize(4, trend_);
      }
      size += getUnknownFields().getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object obj) {
      if (obj == this) {
       return true;
      }
      if (!(obj instanceof NightLiteProto.GlucoseEntry)) {
        return super.equals(obj);
      }
      NightLiteProto.GlucoseEntry other = (NightLiteProto.GlucoseEntry) obj;

      if (getGlucose()
          != other.getGlucose()) return false;
      if (getTimestamp()
          != other.getTimestamp()) return false;
      if (getDelta()
          != other.getDelta()) return false;
      if (trend_ != other.trend_) return false;
      if (!getUnknownFields().equals(other.getUnknownFields())) return false;
      return true;
    }

    @java.lang.Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      hash = (37 * hash) + GLUCOSE_FIELD_NUMBER;
      hash = (53 * hash) + getGlucose();
      hash = (37 * hash) + TIMESTAMP_FIELD_NUMBER;
      hash = (53 * hash) + getTimestamp();
      hash = (37 * hash) + DELTA_FIELD_NUMBER;
      hash = (53 * hash) + getDelta();
      hash = (37 * hash) + TREND_FIELD_NUMBER;
      hash = (53 * hash) + trend_;
      hash = (29 * hash) + getUnknownFields().hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static NightLiteProto.GlucoseEntry parseFrom(
        java.nio.ByteBuffer data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static NightLiteProto.GlucoseEntry parseFrom(
        java.nio.ByteBuffer data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static NightLiteProto.GlucoseEntry parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static NightLiteProto.GlucoseEntry parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static NightLiteProto.GlucoseEntry parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static NightLiteProto.GlucoseEntry parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static NightLiteProto.GlucoseEntry parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseWithIOException(PARSER, input);
    }
    public static NightLiteProto.GlucoseEntry parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static NightLiteProto.GlucoseEntry parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseDelimitedWithIOException(PARSER, input);
    }

    public static NightLiteProto.GlucoseEntry parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }
    public static NightLiteProto.GlucoseEntry parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseWithIOException(PARSER, input);
    }
    public static NightLiteProto.GlucoseEntry parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }
    public static Builder newBuilder(NightLiteProto.GlucoseEntry prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }
    @java.lang.Override
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE
          ? new Builder() : new Builder().mergeFrom(this);
    }

    @java.lang.Override
    protected Builder newBuilderForType(
        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }
    /**
     * Protobuf type {@code GlucoseEntry}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:GlucoseEntry)
        NightLiteProto.GlucoseEntryOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return NightLiteProto.internal_static_GlucoseEntry_descriptor;
      }

      @java.lang.Override
      protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return NightLiteProto.internal_static_GlucoseEntry_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                NightLiteProto.GlucoseEntry.class, NightLiteProto.GlucoseEntry.Builder.class);
      }

      // Construct using NightLiteProto.GlucoseEntry.newBuilder()
      private Builder() {

      }

      private Builder(
          com.google.protobuf.GeneratedMessage.BuilderParent parent) {
        super(parent);

      }
      @java.lang.Override
      public Builder clear() {
        super.clear();
        bitField0_ = 0;
        glucose_ = 0;
        timestamp_ = 0;
        delta_ = 0;
        trend_ = 0;
        return this;
      }

      @java.lang.Override
      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return NightLiteProto.internal_static_GlucoseEntry_descriptor;
      }

      @java.lang.Override
      public NightLiteProto.GlucoseEntry getDefaultInstanceForType() {
        return NightLiteProto.GlucoseEntry.getDefaultInstance();
      }

      @java.lang.Override
      public NightLiteProto.GlucoseEntry build() {
        NightLiteProto.GlucoseEntry result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      @java.lang.Override
      public NightLiteProto.GlucoseEntry buildPartial() {
        NightLiteProto.GlucoseEntry result = new NightLiteProto.GlucoseEntry(this);
        if (bitField0_ != 0) { buildPartial0(result); }
        onBuilt();
        return result;
      }

      private void buildPartial0(NightLiteProto.GlucoseEntry result) {
        int from_bitField0_ = bitField0_;
        if (((from_bitField0_ & 0x00000001) != 0)) {
          result.glucose_ = glucose_;
        }
        if (((from_bitField0_ & 0x00000002) != 0)) {
          result.timestamp_ = timestamp_;
        }
        if (((from_bitField0_ & 0x00000004) != 0)) {
          result.delta_ = delta_;
        }
        if (((from_bitField0_ & 0x00000008) != 0)) {
          result.trend_ = trend_;
        }
      }

      @java.lang.Override
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof NightLiteProto.GlucoseEntry) {
          return mergeFrom((NightLiteProto.GlucoseEntry)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(NightLiteProto.GlucoseEntry other) {
        if (other == NightLiteProto.GlucoseEntry.getDefaultInstance()) return this;
        if (other.getGlucose() != 0) {
          setGlucose(other.getGlucose());
        }
        if (other.getTimestamp() != 0) {
          setTimestamp(other.getTimestamp());
        }
        if (other.getDelta() != 0) {
          setDelta(other.getDelta());
        }
        if (other.trend_ != 0) {
          setTrendValue(other.getTrendValue());
        }
        this.mergeUnknownFields(other.getUnknownFields());
        onChanged();
        return this;
      }

      @java.lang.Override
      public final boolean isInitialized() {
        return true;
      }

      @java.lang.Override
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        if (extensionRegistry == null) {
          throw new java.lang.NullPointerException();
        }
        try {
          boolean done = false;
          while (!done) {
            int tag = input.readTag();
            switch (tag) {
              case 0:
                done = true;
                break;
              case 8: {
                glucose_ = input.readUInt32();
                bitField0_ |= 0x00000001;
                break;
              } // case 8
              case 16: {
                timestamp_ = input.readUInt32();
                bitField0_ |= 0x00000002;
                break;
              } // case 16
              case 24: {
                delta_ = input.readInt32();
                bitField0_ |= 0x00000004;
                break;
              } // case 24
              case 32: {
                trend_ = input.readEnum();
                bitField0_ |= 0x00000008;
                break;
              } // case 32
              default: {
                if (!super.parseUnknownField(input, extensionRegistry, tag)) {
                  done = true; // was an endgroup tag
                }
                break;
              } // default:
            } // switch (tag)
          } // while (!done)
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          throw e.unwrapIOException();
        } finally {
          onChanged();
        } // finally
        return this;
      }
      private int bitField0_;

      private int glucose_ ;
      /**
       * <pre>
       * mgdl
       * </pre>
       *
       * <code>uint32 glucose = 1;</code>
       * @return The glucose.
       */
      @java.lang.Override
      public int getGlucose() {
        return glucose_;
      }
      /**
       * <pre>
       * mgdl
       * </pre>
       *
       * <code>uint32 glucose = 1;</code>
       * @param value The glucose to set.
       * @return This builder for chaining.
       */
      public Builder setGlucose(int value) {

        glucose_ = value;
        bitField0_ |= 0x00000001;
        onChanged();
        return this;
      }
      /**
       * <pre>
       * mgdl
       * </pre>
       *
       * <code>uint32 glucose = 1;</code>
       * @return This builder for chaining.
       */
      public Builder clearGlucose() {
        bitField0_ = (bitField0_ & ~0x00000001);
        glucose_ = 0;
        onChanged();
        return this;
      }

      private int timestamp_ ;
      /**
       * <pre>
       * special epoch seconds
       * </pre>
       *
       * <code>uint32 timestamp = 2;</code>
       * @return The timestamp.
       */
      @java.lang.Override
      public int getTimestamp() {
        return timestamp_;
      }
      /**
       * <pre>
       * special epoch seconds
       * </pre>
       *
       * <code>uint32 timestamp = 2;</code>
       * @param value The timestamp to set.
       * @return This builder for chaining.
       */
      public Builder setTimestamp(int value) {

        timestamp_ = value;
        bitField0_ |= 0x00000002;
        onChanged();
        return this;
      }
      /**
       * <pre>
       * special epoch seconds
       * </pre>
       *
       * <code>uint32 timestamp = 2;</code>
       * @return This builder for chaining.
       */
      public Builder clearTimestamp() {
        bitField0_ = (bitField0_ & ~0x00000002);
        timestamp_ = 0;
        onChanged();
        return this;
      }

      private int delta_ ;
      /**
       * <pre>
       * mgdl * 10
       * </pre>
       *
       * <code>int32 delta = 3;</code>
       * @return The delta.
       */
      @java.lang.Override
      public int getDelta() {
        return delta_;
      }
      /**
       * <pre>
       * mgdl * 10
       * </pre>
       *
       * <code>int32 delta = 3;</code>
       * @param value The delta to set.
       * @return This builder for chaining.
       */
      public Builder setDelta(int value) {

        delta_ = value;
        bitField0_ |= 0x00000004;
        onChanged();
        return this;
      }
      /**
       * <pre>
       * mgdl * 10
       * </pre>
       *
       * <code>int32 delta = 3;</code>
       * @return This builder for chaining.
       */
      public Builder clearDelta() {
        bitField0_ = (bitField0_ & ~0x00000004);
        delta_ = 0;
        onChanged();
        return this;
      }

      private int trend_ = 0;
      /**
       * <code>.Trend trend = 4;</code>
       * @return The enum numeric value on the wire for trend.
       */
      @java.lang.Override public int getTrendValue() {
        return trend_;
      }
      /**
       * <code>.Trend trend = 4;</code>
       * @param value The enum numeric value on the wire for trend to set.
       * @throws IllegalArgumentException if UNRECOGNIZED is provided.
       * @return This builder for chaining.
       */
      public Builder setTrendValue(int value) {
        trend_ = value;
        bitField0_ |= 0x00000008;
        onChanged();
        return this;
      }
      /**
       * <code>.Trend trend = 4;</code>
       * @return The trend.
       */
      @java.lang.Override
      public NightLiteProto.Trend getTrend() {
        NightLiteProto.Trend result = NightLiteProto.Trend.forNumber(trend_);
        return result == null ? NightLiteProto.Trend.UNRECOGNIZED : result;
      }
      /**
       * <code>.Trend trend = 4;</code>
       * @param value The trend to set.
       * @return This builder for chaining.
       */
      public Builder setTrend(NightLiteProto.Trend value) {
        if (value == null) { throw new NullPointerException(); }
        bitField0_ |= 0x00000008;
        trend_ = value.getNumber();
        onChanged();
        return this;
      }
      /**
       * <code>.Trend trend = 4;</code>
       * @return This builder for chaining.
       */
      public Builder clearTrend() {
        bitField0_ = (bitField0_ & ~0x00000008);
        trend_ = 0;
        onChanged();
        return this;
      }

      // @@protoc_insertion_point(builder_scope:GlucoseEntry)
    }

    // @@protoc_insertion_point(class_scope:GlucoseEntry)
    private static final NightLiteProto.GlucoseEntry DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new NightLiteProto.GlucoseEntry();
    }

    public static NightLiteProto.GlucoseEntry getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<GlucoseEntry>
        PARSER = new com.google.protobuf.AbstractParser<GlucoseEntry>() {
      @java.lang.Override
      public GlucoseEntry parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        Builder builder = newBuilder();
        try {
          builder.mergeFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          throw e.setUnfinishedMessage(builder.buildPartial());
        } catch (com.google.protobuf.UninitializedMessageException e) {
          throw e.asInvalidProtocolBufferException().setUnfinishedMessage(builder.buildPartial());
        } catch (java.io.IOException e) {
          throw new com.google.protobuf.InvalidProtocolBufferException(e)
              .setUnfinishedMessage(builder.buildPartial());
        }
        return builder.buildPartial();
      }
    };

    public static com.google.protobuf.Parser<GlucoseEntry> parser() {
      return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<GlucoseEntry> getParserForType() {
      return PARSER;
    }

    @java.lang.Override
    public NightLiteProto.GlucoseEntry getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  public interface TreatmentOrBuilder extends
      // @@protoc_insertion_point(interface_extends:Treatment)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <code>uint32 timestamp = 1;</code>
     * @return The timestamp.
     */
    int getTimestamp();

    /**
     * <code>uint32 carbs = 2;</code>
     * @return The carbs.
     */
    int getCarbs();

    /**
     * <pre>
     * * 100
     * </pre>
     *
     * <code>uint32 units = 3;</code>
     * @return The units.
     */
    int getUnits();
  }
  /**
   * Protobuf type {@code Treatment}
   */
  public static final class Treatment extends
      com.google.protobuf.GeneratedMessage implements
      // @@protoc_insertion_point(message_implements:Treatment)
      TreatmentOrBuilder {
  private static final long serialVersionUID = 0L;
    static {
      com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(
        com.google.protobuf.RuntimeVersion.RuntimeDomain.PUBLIC,
        /* major= */ 4,
        /* minor= */ 34,
        /* patch= */ 0,
        /* suffix= */ "",
        "Treatment");
    }
    // Use Treatment.newBuilder() to construct.
    private Treatment(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
      super(builder);
    }
    private Treatment() {
    }

    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return NightLiteProto.internal_static_Treatment_descriptor;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return NightLiteProto.internal_static_Treatment_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return NightLiteProto.internal_static_Treatment_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              NightLiteProto.Treatment.class, NightLiteProto.Treatment.Builder.class);
    }

    public static final int TIMESTAMP_FIELD_NUMBER = 1;
    private int timestamp_ = 0;
    /**
     * <code>uint32 timestamp = 1;</code>
     * @return The timestamp.
     */
    @java.lang.Override
    public int getTimestamp() {
      return timestamp_;
    }

    public static final int CARBS_FIELD_NUMBER = 2;
    private int carbs_ = 0;
    /**
     * <code>uint32 carbs = 2;</code>
     * @return The carbs.
     */
    @java.lang.Override
    public int getCarbs() {
      return carbs_;
    }

    public static final int UNITS_FIELD_NUMBER = 3;
    private int units_ = 0;
    /**
     * <pre>
     * * 100
     * </pre>
     *
     * <code>uint32 units = 3;</code>
     * @return The units.
     */
    @java.lang.Override
    public int getUnits() {
      return units_;
    }

    private byte memoizedIsInitialized = -1;
    @java.lang.Override
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      memoizedIsInitialized = 1;
      return true;
    }

    @java.lang.Override
    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      if (timestamp_ != 0) {
        output.writeUInt32(1, timestamp_);
      }
      if (carbs_ != 0) {
        output.writeUInt32(2, carbs_);
      }
      if (units_ != 0) {
        output.writeUInt32(3, units_);
      }
      getUnknownFields().writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      if (timestamp_ != 0) {
        size += com.google.protobuf.CodedOutputStream
          .computeUInt32Size(1, timestamp_);
      }
      if (carbs_ != 0) {
        size += com.google.protobuf.CodedOutputStream
          .computeUInt32Size(2, carbs_);
      }
      if (units_ != 0) {
        size += com.google.protobuf.CodedOutputStream
          .computeUInt32Size(3, units_);
      }
      size += getUnknownFields().getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object obj) {
      if (obj == this) {
       return true;
      }
      if (!(obj instanceof NightLiteProto.Treatment)) {
        return super.equals(obj);
      }
      NightLiteProto.Treatment other = (NightLiteProto.Treatment) obj;

      if (getTimestamp()
          != other.getTimestamp()) return false;
      if (getCarbs()
          != other.getCarbs()) return false;
      if (getUnits()
          != other.getUnits()) return false;
      if (!getUnknownFields().equals(other.getUnknownFields())) return false;
      return true;
    }

    @java.lang.Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      hash = (37 * hash) + TIMESTAMP_FIELD_NUMBER;
      hash = (53 * hash) + getTimestamp();
      hash = (37 * hash) + CARBS_FIELD_NUMBER;
      hash = (53 * hash) + getCarbs();
      hash = (37 * hash) + UNITS_FIELD_NUMBER;
      hash = (53 * hash) + getUnits();
      hash = (29 * hash) + getUnknownFields().hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static NightLiteProto.Treatment parseFrom(
        java.nio.ByteBuffer data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static NightLiteProto.Treatment parseFrom(
        java.nio.ByteBuffer data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static NightLiteProto.Treatment parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static NightLiteProto.Treatment parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static NightLiteProto.Treatment parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static NightLiteProto.Treatment parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static NightLiteProto.Treatment parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseWithIOException(PARSER, input);
    }
    public static NightLiteProto.Treatment parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static NightLiteProto.Treatment parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseDelimitedWithIOException(PARSER, input);
    }

    public static NightLiteProto.Treatment parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }
    public static NightLiteProto.Treatment parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseWithIOException(PARSER, input);
    }
    public static NightLiteProto.Treatment parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }
    public static Builder newBuilder(NightLiteProto.Treatment prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }
    @java.lang.Override
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE
          ? new Builder() : new Builder().mergeFrom(this);
    }

    @java.lang.Override
    protected Builder newBuilderForType(
        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }
    /**
     * Protobuf type {@code Treatment}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:Treatment)
        NightLiteProto.TreatmentOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return NightLiteProto.internal_static_Treatment_descriptor;
      }

      @java.lang.Override
      protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return NightLiteProto.internal_static_Treatment_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                NightLiteProto.Treatment.class, NightLiteProto.Treatment.Builder.class);
      }

      // Construct using NightLiteProto.Treatment.newBuilder()
      private Builder() {

      }

      private Builder(
          com.google.protobuf.GeneratedMessage.BuilderParent parent) {
        super(parent);

      }
      @java.lang.Override
      public Builder clear() {
        super.clear();
        bitField0_ = 0;
        timestamp_ = 0;
        carbs_ = 0;
        units_ = 0;
        return this;
      }

      @java.lang.Override
      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return NightLiteProto.internal_static_Treatment_descriptor;
      }

      @java.lang.Override
      public NightLiteProto.Treatment getDefaultInstanceForType() {
        return NightLiteProto.Treatment.getDefaultInstance();
      }

      @java.lang.Override
      public NightLiteProto.Treatment build() {
        NightLiteProto.Treatment result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      @java.lang.Override
      public NightLiteProto.Treatment buildPartial() {
        NightLiteProto.Treatment result = new NightLiteProto.Treatment(this);
        if (bitField0_ != 0) { buildPartial0(result); }
        onBuilt();
        return result;
      }

      private void buildPartial0(NightLiteProto.Treatment result) {
        int from_bitField0_ = bitField0_;
        if (((from_bitField0_ & 0x00000001) != 0)) {
          result.timestamp_ = timestamp_;
        }
        if (((from_bitField0_ & 0x00000002) != 0)) {
          result.carbs_ = carbs_;
        }
        if (((from_bitField0_ & 0x00000004) != 0)) {
          result.units_ = units_;
        }
      }

      @java.lang.Override
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof NightLiteProto.Treatment) {
          return mergeFrom((NightLiteProto.Treatment)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(NightLiteProto.Treatment other) {
        if (other == NightLiteProto.Treatment.getDefaultInstance()) return this;
        if (other.getTimestamp() != 0) {
          setTimestamp(other.getTimestamp());
        }
        if (other.getCarbs() != 0) {
          setCarbs(other.getCarbs());
        }
        if (other.getUnits() != 0) {
          setUnits(other.getUnits());
        }
        this.mergeUnknownFields(other.getUnknownFields());
        onChanged();
        return this;
      }

      @java.lang.Override
      public final boolean isInitialized() {
        return true;
      }

      @java.lang.Override
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        if (extensionRegistry == null) {
          throw new java.lang.NullPointerException();
        }
        try {
          boolean done = false;
          while (!done) {
            int tag = input.readTag();
            switch (tag) {
              case 0:
                done = true;
                break;
              case 8: {
                timestamp_ = input.readUInt32();
                bitField0_ |= 0x00000001;
                break;
              } // case 8
              case 16: {
                carbs_ = input.readUInt32();
                bitField0_ |= 0x00000002;
                break;
              } // case 16
              case 24: {
                units_ = input.readUInt32();
                bitField0_ |= 0x00000004;
                break;
              } // case 24
              default: {
                if (!super.parseUnknownField(input, extensionRegistry, tag)) {
                  done = true; // was an endgroup tag
                }
                break;
              } // default:
            } // switch (tag)
          } // while (!done)
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          throw e.unwrapIOException();
        } finally {
          onChanged();
        } // finally
        return this;
      }
      private int bitField0_;

      private int timestamp_ ;
      /**
       * <code>uint32 timestamp = 1;</code>
       * @return The timestamp.
       */
      @java.lang.Override
      public int getTimestamp() {
        return timestamp_;
      }
      /**
       * <code>uint32 timestamp = 1;</code>
       * @param value The timestamp to set.
       * @return This builder for chaining.
       */
      public Builder setTimestamp(int value) {

        timestamp_ = value;
        bitField0_ |= 0x00000001;
        onChanged();
        return this;
      }
      /**
       * <code>uint32 timestamp = 1;</code>
       * @return This builder for chaining.
       */
      public Builder clearTimestamp() {
        bitField0_ = (bitField0_ & ~0x00000001);
        timestamp_ = 0;
        onChanged();
        return this;
      }

      private int carbs_ ;
      /**
       * <code>uint32 carbs = 2;</code>
       * @return The carbs.
       */
      @java.lang.Override
      public int getCarbs() {
        return carbs_;
      }
      /**
       * <code>uint32 carbs = 2;</code>
       * @param value The carbs to set.
       * @return This builder for chaining.
       */
      public Builder setCarbs(int value) {

        carbs_ = value;
        bitField0_ |= 0x00000002;
        onChanged();
        return this;
      }
      /**
       * <code>uint32 carbs = 2;</code>
       * @return This builder for chaining.
       */
      public Builder clearCarbs() {
        bitField0_ = (bitField0_ & ~0x00000002);
        carbs_ = 0;
        onChanged();
        return this;
      }

      private int units_ ;
      /**
       * <pre>
       * * 100
       * </pre>
       *
       * <code>uint32 units = 3;</code>
       * @return The units.
       */
      @java.lang.Override
      public int getUnits() {
        return units_;
      }
      /**
       * <pre>
       * * 100
       * </pre>
       *
       * <code>uint32 units = 3;</code>
       * @param value The units to set.
       * @return This builder for chaining.
       */
      public Builder setUnits(int value) {

        units_ = value;
        bitField0_ |= 0x00000004;
        onChanged();
        return this;
      }
      /**
       * <pre>
       * * 100
       * </pre>
       *
       * <code>uint32 units = 3;</code>
       * @return This builder for chaining.
       */
      public Builder clearUnits() {
        bitField0_ = (bitField0_ & ~0x00000004);
        units_ = 0;
        onChanged();
        return this;
      }

      // @@protoc_insertion_point(builder_scope:Treatment)
    }

    // @@protoc_insertion_point(class_scope:Treatment)
    private static final NightLiteProto.Treatment DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new NightLiteProto.Treatment();
    }

    public static NightLiteProto.Treatment getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<Treatment>
        PARSER = new com.google.protobuf.AbstractParser<Treatment>() {
      @java.lang.Override
      public Treatment parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        Builder builder = newBuilder();
        try {
          builder.mergeFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          throw e.setUnfinishedMessage(builder.buildPartial());
        } catch (com.google.protobuf.UninitializedMessageException e) {
          throw e.asInvalidProtocolBufferException().setUnfinishedMessage(builder.buildPartial());
        } catch (java.io.IOException e) {
          throw new com.google.protobuf.InvalidProtocolBufferException(e)
              .setUnfinishedMessage(builder.buildPartial());
        }
        return builder.buildPartial();
      }
    };

    public static com.google.protobuf.Parser<Treatment> parser() {
      return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<Treatment> getParserForType() {
      return PARSER;
    }

    @java.lang.Override
    public NightLiteProto.Treatment getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  public interface KeyValuePairOrBuilder extends
      // @@protoc_insertion_point(interface_extends:KeyValuePair)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <code>string key = 1;</code>
     * @return The key.
     */
    java.lang.String getKey();
    /**
     * <code>string key = 1;</code>
     * @return The bytes for key.
     */
    com.google.protobuf.ByteString
        getKeyBytes();

    /**
     * <code>string value = 2;</code>
     * @return The value.
     */
    java.lang.String getValue();
    /**
     * <code>string value = 2;</code>
     * @return The bytes for value.
     */
    com.google.protobuf.ByteString
        getValueBytes();
  }
  /**
   * Protobuf type {@code KeyValuePair}
   */
  public static final class KeyValuePair extends
      com.google.protobuf.GeneratedMessage implements
      // @@protoc_insertion_point(message_implements:KeyValuePair)
      KeyValuePairOrBuilder {
  private static final long serialVersionUID = 0L;
    static {
      com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(
        com.google.protobuf.RuntimeVersion.RuntimeDomain.PUBLIC,
        /* major= */ 4,
        /* minor= */ 34,
        /* patch= */ 0,
        /* suffix= */ "",
        "KeyValuePair");
    }
    // Use KeyValuePair.newBuilder() to construct.
    private KeyValuePair(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
      super(builder);
    }
    private KeyValuePair() {
      key_ = "";
      value_ = "";
    }

    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return NightLiteProto.internal_static_KeyValuePair_descriptor;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return NightLiteProto.internal_static_KeyValuePair_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return NightLiteProto.internal_static_KeyValuePair_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              NightLiteProto.KeyValuePair.class, NightLiteProto.KeyValuePair.Builder.class);
    }

    public static final int KEY_FIELD_NUMBER = 1;
    @SuppressWarnings("serial")
    private volatile java.lang.Object key_ = "";
    /**
     * <code>string key = 1;</code>
     * @return The key.
     */
    @java.lang.Override
    public java.lang.String getKey() {
      java.lang.Object ref = key_;
      if (ref instanceof java.lang.String) {
        return (java.lang.String) ref;
      } else {
        com.google.protobuf.ByteString bs = 
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        key_ = s;
        return s;
      }
    }
    /**
     * <code>string key = 1;</code>
     * @return The bytes for key.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
        getKeyBytes() {
      java.lang.Object ref = key_;
      if (ref instanceof java.lang.String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        key_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int VALUE_FIELD_NUMBER = 2;
    @SuppressWarnings("serial")
    private volatile java.lang.Object value_ = "";
    /**
     * <code>string value = 2;</code>
     * @return The value.
     */
    @java.lang.Override
    public java.lang.String getValue() {
      java.lang.Object ref = value_;
      if (ref instanceof java.lang.String) {
        return (java.lang.String) ref;
      } else {
        com.google.protobuf.ByteString bs = 
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        value_ = s;
        return s;
      }
    }
    /**
     * <code>string value = 2;</code>
     * @return The bytes for value.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
        getValueBytes() {
      java.lang.Object ref = value_;
      if (ref instanceof java.lang.String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        value_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    private byte memoizedIsInitialized = -1;
    @java.lang.Override
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      memoizedIsInitialized = 1;
      return true;
    }

    @java.lang.Override
    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      if (!com.google.protobuf.GeneratedMessage.isStringEmpty(key_)) {
        com.google.protobuf.GeneratedMessage.writeString(output, 1, key_);
      }
      if (!com.google.protobuf.GeneratedMessage.isStringEmpty(value_)) {
        com.google.protobuf.GeneratedMessage.writeString(output, 2, value_);
      }
      getUnknownFields().writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      if (!com.google.protobuf.GeneratedMessage.isStringEmpty(key_)) {
        size += com.google.protobuf.GeneratedMessage.computeStringSize(1, key_);
      }
      if (!com.google.protobuf.GeneratedMessage.isStringEmpty(value_)) {
        size += com.google.protobuf.GeneratedMessage.computeStringSize(2, value_);
      }
      size += getUnknownFields().getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object obj) {
      if (obj == this) {
       return true;
      }
      if (!(obj instanceof NightLiteProto.KeyValuePair)) {
        return super.equals(obj);
      }
      NightLiteProto.KeyValuePair other = (NightLiteProto.KeyValuePair) obj;

      if (!getKey()
          .equals(other.getKey())) return false;
      if (!getValue()
          .equals(other.getValue())) return false;
      if (!getUnknownFields().equals(other.getUnknownFields())) return false;
      return true;
    }

    @java.lang.Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      hash = (37 * hash) + KEY_FIELD_NUMBER;
      hash = (53 * hash) + getKey().hashCode();
      hash = (37 * hash) + VALUE_FIELD_NUMBER;
      hash = (53 * hash) + getValue().hashCode();
      hash = (29 * hash) + getUnknownFields().hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static NightLiteProto.KeyValuePair parseFrom(
        java.nio.ByteBuffer data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static NightLiteProto.KeyValuePair parseFrom(
        java.nio.ByteBuffer data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static NightLiteProto.KeyValuePair parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static NightLiteProto.KeyValuePair parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static NightLiteProto.KeyValuePair parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static NightLiteProto.KeyValuePair parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static NightLiteProto.KeyValuePair parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseWithIOException(PARSER, input);
    }
    public static NightLiteProto.KeyValuePair parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static NightLiteProto.KeyValuePair parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseDelimitedWithIOException(PARSER, input);
    }

    public static NightLiteProto.KeyValuePair parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }
    public static NightLiteProto.KeyValuePair parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseWithIOException(PARSER, input);
    }
    public static NightLiteProto.KeyValuePair parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }
    public static Builder newBuilder(NightLiteProto.KeyValuePair prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }
    @java.lang.Override
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE
          ? new Builder() : new Builder().mergeFrom(this);
    }

    @java.lang.Override
    protected Builder newBuilderForType(
        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }
    /**
     * Protobuf type {@code KeyValuePair}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:KeyValuePair)
        NightLiteProto.KeyValuePairOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return NightLiteProto.internal_static_KeyValuePair_descriptor;
      }

      @java.lang.Override
      protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return NightLiteProto.internal_static_KeyValuePair_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                NightLiteProto.KeyValuePair.class, NightLiteProto.KeyValuePair.Builder.class);
      }

      // Construct using NightLiteProto.KeyValuePair.newBuilder()
      private Builder() {

      }

      private Builder(
          com.google.protobuf.GeneratedMessage.BuilderParent parent) {
        super(parent);

      }
      @java.lang.Override
      public Builder clear() {
        super.clear();
        bitField0_ = 0;
        key_ = "";
        value_ = "";
        return this;
      }

      @java.lang.Override
      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return NightLiteProto.internal_static_KeyValuePair_descriptor;
      }

      @java.lang.Override
      public NightLiteProto.KeyValuePair getDefaultInstanceForType() {
        return NightLiteProto.KeyValuePair.getDefaultInstance();
      }

      @java.lang.Override
      public NightLiteProto.KeyValuePair build() {
        NightLiteProto.KeyValuePair result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      @java.lang.Override
      public NightLiteProto.KeyValuePair buildPartial() {
        NightLiteProto.KeyValuePair result = new NightLiteProto.KeyValuePair(this);
        if (bitField0_ != 0) { buildPartial0(result); }
        onBuilt();
        return result;
      }

      private void buildPartial0(NightLiteProto.KeyValuePair result) {
        int from_bitField0_ = bitField0_;
        if (((from_bitField0_ & 0x00000001) != 0)) {
          result.key_ = key_;
        }
        if (((from_bitField0_ & 0x00000002) != 0)) {
          result.value_ = value_;
        }
      }

      @java.lang.Override
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof NightLiteProto.KeyValuePair) {
          return mergeFrom((NightLiteProto.KeyValuePair)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(NightLiteProto.KeyValuePair other) {
        if (other == NightLiteProto.KeyValuePair.getDefaultInstance()) return this;
        if (!other.getKey().isEmpty()) {
          key_ = other.key_;
          bitField0_ |= 0x00000001;
          onChanged();
        }
        if (!other.getValue().isEmpty()) {
          value_ = other.value_;
          bitField0_ |= 0x00000002;
          onChanged();
        }
        this.mergeUnknownFields(other.getUnknownFields());
        onChanged();
        return this;
      }

      @java.lang.Override
      public final boolean isInitialized() {
        return true;
      }

      @java.lang.Override
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        if (extensionRegistry == null) {
          throw new java.lang.NullPointerException();
        }
        try {
          boolean done = false;
          while (!done) {
            int tag = input.readTag();
            switch (tag) {
              case 0:
                done = true;
                break;
              case 10: {
                key_ = input.readStringRequireUtf8();
                bitField0_ |= 0x00000001;
                break;
              } // case 10
              case 18: {
                value_ = input.readStringRequireUtf8();
                bitField0_ |= 0x00000002;
                break;
              } // case 18
              default: {
                if (!super.parseUnknownField(input, extensionRegistry, tag)) {
                  done = true; // was an endgroup tag
                }
                break;
              } // default:
            } // switch (tag)
          } // while (!done)
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          throw e.unwrapIOException();
        } finally {
          onChanged();
        } // finally
        return this;
      }
      private int bitField0_;

      private java.lang.Object key_ = "";
      /**
       * <code>string key = 1;</code>
       * @return The key.
       */
      public java.lang.String getKey() {
        java.lang.Object ref = key_;
        if (!(ref instanceof java.lang.String)) {
          com.google.protobuf.ByteString bs =
              (com.google.protobuf.ByteString) ref;
          java.lang.String s = bs.toStringUtf8();
          key_ = s;
          return s;
        } else {
          return (java.lang.String) ref;
        }
      }
      /**
       * <code>string key = 1;</code>
       * @return The bytes for key.
       */
      public com.google.protobuf.ByteString
          getKeyBytes() {
        java.lang.Object ref = key_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = 
              com.google.protobuf.ByteString.copyFromUtf8(
                  (java.lang.String) ref);
          key_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }
      /**
       * <code>string key = 1;</code>
       * @param value The key to set.
       * @return This builder for chaining.
       */
      public Builder setKey(
          java.lang.String value) {
        if (value == null) { throw new NullPointerException(); }
        key_ = value;
        bitField0_ |= 0x00000001;
        onChanged();
        return this;
      }
      /**
       * <code>string key = 1;</code>
       * @return This builder for chaining.
       */
      public Builder clearKey() {
        key_ = getDefaultInstance().getKey();
        bitField0_ = (bitField0_ & ~0x00000001);
        onChanged();
        return this;
      }
      /**
       * <code>string key = 1;</code>
       * @param value The bytes for key to set.
       * @return This builder for chaining.
       */
      public Builder setKeyBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) { throw new NullPointerException(); }
        checkByteStringIsUtf8(value);
        key_ = value;
        bitField0_ |= 0x00000001;
        onChanged();
        return this;
      }

      private java.lang.Object value_ = "";
      /**
       * <code>string value = 2;</code>
       * @return The value.
       */
      public java.lang.String getValue() {
        java.lang.Object ref = value_;
        if (!(ref instanceof java.lang.String)) {
          com.google.protobuf.ByteString bs =
              (com.google.protobuf.ByteString) ref;
          java.lang.String s = bs.toStringUtf8();
          value_ = s;
          return s;
        } else {
          return (java.lang.String) ref;
        }
      }
      /**
       * <code>string value = 2;</code>
       * @return The bytes for value.
       */
      public com.google.protobuf.ByteString
          getValueBytes() {
        java.lang.Object ref = value_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = 
              com.google.protobuf.ByteString.copyFromUtf8(
                  (java.lang.String) ref);
          value_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }
      /**
       * <code>string value = 2;</code>
       * @param value The value to set.
       * @return This builder for chaining.
       */
      public Builder setValue(
          java.lang.String value) {
        if (value == null) { throw new NullPointerException(); }
        value_ = value;
        bitField0_ |= 0x00000002;
        onChanged();
        return this;
      }
      /**
       * <code>string value = 2;</code>
       * @return This builder for chaining.
       */
      public Builder clearValue() {
        value_ = getDefaultInstance().getValue();
        bitField0_ = (bitField0_ & ~0x00000002);
        onChanged();
        return this;
      }
      /**
       * <code>string value = 2;</code>
       * @param value The bytes for value to set.
       * @return This builder for chaining.
       */
      public Builder setValueBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) { throw new NullPointerException(); }
        checkByteStringIsUtf8(value);
        value_ = value;
        bitField0_ |= 0x00000002;
        onChanged();
        return this;
      }

      // @@protoc_insertion_point(builder_scope:KeyValuePair)
    }

    // @@protoc_insertion_point(class_scope:KeyValuePair)
    private static final NightLiteProto.KeyValuePair DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new NightLiteProto.KeyValuePair();
    }

    public static NightLiteProto.KeyValuePair getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<KeyValuePair>
        PARSER = new com.google.protobuf.AbstractParser<KeyValuePair>() {
      @java.lang.Override
      public KeyValuePair parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        Builder builder = newBuilder();
        try {
          builder.mergeFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          throw e.setUnfinishedMessage(builder.buildPartial());
        } catch (com.google.protobuf.UninitializedMessageException e) {
          throw e.asInvalidProtocolBufferException().setUnfinishedMessage(builder.buildPartial());
        } catch (java.io.IOException e) {
          throw new com.google.protobuf.InvalidProtocolBufferException(e)
              .setUnfinishedMessage(builder.buildPartial());
        }
        return builder.buildPartial();
      }
    };

    public static com.google.protobuf.Parser<KeyValuePair> parser() {
      return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<KeyValuePair> getParserForType() {
      return PARSER;
    }

    @java.lang.Override
    public NightLiteProto.KeyValuePair getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  public interface SpecialOrBuilder extends
      // @@protoc_insertion_point(interface_extends:Special)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <code>uint32 timestamp = 1;</code>
     * @return The timestamp.
     */
    int getTimestamp();

    /**
     * <code>string msg = 2;</code>
     * @return The msg.
     */
    java.lang.String getMsg();
    /**
     * <code>string msg = 2;</code>
     * @return The bytes for msg.
     */
    com.google.protobuf.ByteString
        getMsgBytes();
  }
  /**
   * Protobuf type {@code Special}
   */
  public static final class Special extends
      com.google.protobuf.GeneratedMessage implements
      // @@protoc_insertion_point(message_implements:Special)
      SpecialOrBuilder {
  private static final long serialVersionUID = 0L;
    static {
      com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(
        com.google.protobuf.RuntimeVersion.RuntimeDomain.PUBLIC,
        /* major= */ 4,
        /* minor= */ 34,
        /* patch= */ 0,
        /* suffix= */ "",
        "Special");
    }
    // Use Special.newBuilder() to construct.
    private Special(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
      super(builder);
    }
    private Special() {
      msg_ = "";
    }

    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return NightLiteProto.internal_static_Special_descriptor;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return NightLiteProto.internal_static_Special_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return NightLiteProto.internal_static_Special_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              NightLiteProto.Special.class, NightLiteProto.Special.Builder.class);
    }

    public static final int TIMESTAMP_FIELD_NUMBER = 1;
    private int timestamp_ = 0;
    /**
     * <code>uint32 timestamp = 1;</code>
     * @return The timestamp.
     */
    @java.lang.Override
    public int getTimestamp() {
      return timestamp_;
    }

    public static final int MSG_FIELD_NUMBER = 2;
    @SuppressWarnings("serial")
    private volatile java.lang.Object msg_ = "";
    /**
     * <code>string msg = 2;</code>
     * @return The msg.
     */
    @java.lang.Override
    public java.lang.String getMsg() {
      java.lang.Object ref = msg_;
      if (ref instanceof java.lang.String) {
        return (java.lang.String) ref;
      } else {
        com.google.protobuf.ByteString bs = 
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        msg_ = s;
        return s;
      }
    }
    /**
     * <code>string msg = 2;</code>
     * @return The bytes for msg.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
        getMsgBytes() {
      java.lang.Object ref = msg_;
      if (ref instanceof java.lang.String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        msg_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    private byte memoizedIsInitialized = -1;
    @java.lang.Override
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      memoizedIsInitialized = 1;
      return true;
    }

    @java.lang.Override
    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      if (timestamp_ != 0) {
        output.writeUInt32(1, timestamp_);
      }
      if (!com.google.protobuf.GeneratedMessage.isStringEmpty(msg_)) {
        com.google.protobuf.GeneratedMessage.writeString(output, 2, msg_);
      }
      getUnknownFields().writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      if (timestamp_ != 0) {
        size += com.google.protobuf.CodedOutputStream
          .computeUInt32Size(1, timestamp_);
      }
      if (!com.google.protobuf.GeneratedMessage.isStringEmpty(msg_)) {
        size += com.google.protobuf.GeneratedMessage.computeStringSize(2, msg_);
      }
      size += getUnknownFields().getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object obj) {
      if (obj == this) {
       return true;
      }
      if (!(obj instanceof NightLiteProto.Special)) {
        return super.equals(obj);
      }
      NightLiteProto.Special other = (NightLiteProto.Special) obj;

      if (getTimestamp()
          != other.getTimestamp()) return false;
      if (!getMsg()
          .equals(other.getMsg())) return false;
      if (!getUnknownFields().equals(other.getUnknownFields())) return false;
      return true;
    }

    @java.lang.Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      hash = (37 * hash) + TIMESTAMP_FIELD_NUMBER;
      hash = (53 * hash) + getTimestamp();
      hash = (37 * hash) + MSG_FIELD_NUMBER;
      hash = (53 * hash) + getMsg().hashCode();
      hash = (29 * hash) + getUnknownFields().hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static NightLiteProto.Special parseFrom(
        java.nio.ByteBuffer data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static NightLiteProto.Special parseFrom(
        java.nio.ByteBuffer data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static NightLiteProto.Special parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static NightLiteProto.Special parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static NightLiteProto.Special parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static NightLiteProto.Special parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static NightLiteProto.Special parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseWithIOException(PARSER, input);
    }
    public static NightLiteProto.Special parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static NightLiteProto.Special parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseDelimitedWithIOException(PARSER, input);
    }

    public static NightLiteProto.Special parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }
    public static NightLiteProto.Special parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseWithIOException(PARSER, input);
    }
    public static NightLiteProto.Special parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }
    public static Builder newBuilder(NightLiteProto.Special prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }
    @java.lang.Override
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE
          ? new Builder() : new Builder().mergeFrom(this);
    }

    @java.lang.Override
    protected Builder newBuilderForType(
        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }
    /**
     * Protobuf type {@code Special}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:Special)
        NightLiteProto.SpecialOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return NightLiteProto.internal_static_Special_descriptor;
      }

      @java.lang.Override
      protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return NightLiteProto.internal_static_Special_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                NightLiteProto.Special.class, NightLiteProto.Special.Builder.class);
      }

      // Construct using NightLiteProto.Special.newBuilder()
      private Builder() {

      }

      private Builder(
          com.google.protobuf.GeneratedMessage.BuilderParent parent) {
        super(parent);

      }
      @java.lang.Override
      public Builder clear() {
        super.clear();
        bitField0_ = 0;
        timestamp_ = 0;
        msg_ = "";
        return this;
      }

      @java.lang.Override
      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return NightLiteProto.internal_static_Special_descriptor;
      }

      @java.lang.Override
      public NightLiteProto.Special getDefaultInstanceForType() {
        return NightLiteProto.Special.getDefaultInstance();
      }

      @java.lang.Override
      public NightLiteProto.Special build() {
        NightLiteProto.Special result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      @java.lang.Override
      public NightLiteProto.Special buildPartial() {
        NightLiteProto.Special result = new NightLiteProto.Special(this);
        if (bitField0_ != 0) { buildPartial0(result); }
        onBuilt();
        return result;
      }

      private void buildPartial0(NightLiteProto.Special result) {
        int from_bitField0_ = bitField0_;
        if (((from_bitField0_ & 0x00000001) != 0)) {
          result.timestamp_ = timestamp_;
        }
        if (((from_bitField0_ & 0x00000002) != 0)) {
          result.msg_ = msg_;
        }
      }

      @java.lang.Override
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof NightLiteProto.Special) {
          return mergeFrom((NightLiteProto.Special)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(NightLiteProto.Special other) {
        if (other == NightLiteProto.Special.getDefaultInstance()) return this;
        if (other.getTimestamp() != 0) {
          setTimestamp(other.getTimestamp());
        }
        if (!other.getMsg().isEmpty()) {
          msg_ = other.msg_;
          bitField0_ |= 0x00000002;
          onChanged();
        }
        this.mergeUnknownFields(other.getUnknownFields());
        onChanged();
        return this;
      }

      @java.lang.Override
      public final boolean isInitialized() {
        return true;
      }

      @java.lang.Override
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        if (extensionRegistry == null) {
          throw new java.lang.NullPointerException();
        }
        try {
          boolean done = false;
          while (!done) {
            int tag = input.readTag();
            switch (tag) {
              case 0:
                done = true;
                break;
              case 8: {
                timestamp_ = input.readUInt32();
                bitField0_ |= 0x00000001;
                break;
              } // case 8
              case 18: {
                msg_ = input.readStringRequireUtf8();
                bitField0_ |= 0x00000002;
                break;
              } // case 18
              default: {
                if (!super.parseUnknownField(input, extensionRegistry, tag)) {
                  done = true; // was an endgroup tag
                }
                break;
              } // default:
            } // switch (tag)
          } // while (!done)
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          throw e.unwrapIOException();
        } finally {
          onChanged();
        } // finally
        return this;
      }
      private int bitField0_;

      private int timestamp_ ;
      /**
       * <code>uint32 timestamp = 1;</code>
       * @return The timestamp.
       */
      @java.lang.Override
      public int getTimestamp() {
        return timestamp_;
      }
      /**
       * <code>uint32 timestamp = 1;</code>
       * @param value The timestamp to set.
       * @return This builder for chaining.
       */
      public Builder setTimestamp(int value) {

        timestamp_ = value;
        bitField0_ |= 0x00000001;
        onChanged();
        return this;
      }
      /**
       * <code>uint32 timestamp = 1;</code>
       * @return This builder for chaining.
       */
      public Builder clearTimestamp() {
        bitField0_ = (bitField0_ & ~0x00000001);
        timestamp_ = 0;
        onChanged();
        return this;
      }

      private java.lang.Object msg_ = "";
      /**
       * <code>string msg = 2;</code>
       * @return The msg.
       */
      public java.lang.String getMsg() {
        java.lang.Object ref = msg_;
        if (!(ref instanceof java.lang.String)) {
          com.google.protobuf.ByteString bs =
              (com.google.protobuf.ByteString) ref;
          java.lang.String s = bs.toStringUtf8();
          msg_ = s;
          return s;
        } else {
          return (java.lang.String) ref;
        }
      }
      /**
       * <code>string msg = 2;</code>
       * @return The bytes for msg.
       */
      public com.google.protobuf.ByteString
          getMsgBytes() {
        java.lang.Object ref = msg_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = 
              com.google.protobuf.ByteString.copyFromUtf8(
                  (java.lang.String) ref);
          msg_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }
      /**
       * <code>string msg = 2;</code>
       * @param value The msg to set.
       * @return This builder for chaining.
       */
      public Builder setMsg(
          java.lang.String value) {
        if (value == null) { throw new NullPointerException(); }
        msg_ = value;
        bitField0_ |= 0x00000002;
        onChanged();
        return this;
      }
      /**
       * <code>string msg = 2;</code>
       * @return This builder for chaining.
       */
      public Builder clearMsg() {
        msg_ = getDefaultInstance().getMsg();
        bitField0_ = (bitField0_ & ~0x00000002);
        onChanged();
        return this;
      }
      /**
       * <code>string msg = 2;</code>
       * @param value The bytes for msg to set.
       * @return This builder for chaining.
       */
      public Builder setMsgBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) { throw new NullPointerException(); }
        checkByteStringIsUtf8(value);
        msg_ = value;
        bitField0_ |= 0x00000002;
        onChanged();
        return this;
      }

      // @@protoc_insertion_point(builder_scope:Special)
    }

    // @@protoc_insertion_point(class_scope:Special)
    private static final NightLiteProto.Special DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new NightLiteProto.Special();
    }

    public static NightLiteProto.Special getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<Special>
        PARSER = new com.google.protobuf.AbstractParser<Special>() {
      @java.lang.Override
      public Special parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        Builder builder = newBuilder();
        try {
          builder.mergeFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          throw e.setUnfinishedMessage(builder.buildPartial());
        } catch (com.google.protobuf.UninitializedMessageException e) {
          throw e.asInvalidProtocolBufferException().setUnfinishedMessage(builder.buildPartial());
        } catch (java.io.IOException e) {
          throw new com.google.protobuf.InvalidProtocolBufferException(e)
              .setUnfinishedMessage(builder.buildPartial());
        }
        return builder.buildPartial();
      }
    };

    public static com.google.protobuf.Parser<Special> parser() {
      return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<Special> getParserForType() {
      return PARSER;
    }

    @java.lang.Override
    public NightLiteProto.Special getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  public interface BlockOrBuilder extends
      // @@protoc_insertion_point(interface_extends:Block)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <code>repeated .GlucoseEntry entries = 1;</code>
     */
    java.util.List<NightLiteProto.GlucoseEntry> 
        getEntriesList();
    /**
     * <code>repeated .GlucoseEntry entries = 1;</code>
     */
    NightLiteProto.GlucoseEntry getEntries(int index);
    /**
     * <code>repeated .GlucoseEntry entries = 1;</code>
     */
    int getEntriesCount();
    /**
     * <code>repeated .GlucoseEntry entries = 1;</code>
     */
    java.util.List<? extends NightLiteProto.GlucoseEntryOrBuilder> 
        getEntriesOrBuilderList();
    /**
     * <code>repeated .GlucoseEntry entries = 1;</code>
     */
    NightLiteProto.GlucoseEntryOrBuilder getEntriesOrBuilder(
        int index);

    /**
     * <code>repeated .Treatment treatments = 2;</code>
     */
    java.util.List<NightLiteProto.Treatment> 
        getTreatmentsList();
    /**
     * <code>repeated .Treatment treatments = 2;</code>
     */
    NightLiteProto.Treatment getTreatments(int index);
    /**
     * <code>repeated .Treatment treatments = 2;</code>
     */
    int getTreatmentsCount();
    /**
     * <code>repeated .Treatment treatments = 2;</code>
     */
    java.util.List<? extends NightLiteProto.TreatmentOrBuilder> 
        getTreatmentsOrBuilderList();
    /**
     * <code>repeated .Treatment treatments = 2;</code>
     */
    NightLiteProto.TreatmentOrBuilder getTreatmentsOrBuilder(
        int index);

    /**
     * <code>repeated .Special special = 3;</code>
     */
    java.util.List<NightLiteProto.Special> 
        getSpecialList();
    /**
     * <code>repeated .Special special = 3;</code>
     */
    NightLiteProto.Special getSpecial(int index);
    /**
     * <code>repeated .Special special = 3;</code>
     */
    int getSpecialCount();
    /**
     * <code>repeated .Special special = 3;</code>
     */
    java.util.List<? extends NightLiteProto.SpecialOrBuilder> 
        getSpecialOrBuilderList();
    /**
     * <code>repeated .Special special = 3;</code>
     */
    NightLiteProto.SpecialOrBuilder getSpecialOrBuilder(
        int index);

    /**
     * <code>repeated .KeyValuePair meta = 4;</code>
     */
    java.util.List<NightLiteProto.KeyValuePair> 
        getMetaList();
    /**
     * <code>repeated .KeyValuePair meta = 4;</code>
     */
    NightLiteProto.KeyValuePair getMeta(int index);
    /**
     * <code>repeated .KeyValuePair meta = 4;</code>
     */
    int getMetaCount();
    /**
     * <code>repeated .KeyValuePair meta = 4;</code>
     */
    java.util.List<? extends NightLiteProto.KeyValuePairOrBuilder> 
        getMetaOrBuilderList();
    /**
     * <code>repeated .KeyValuePair meta = 4;</code>
     */
    NightLiteProto.KeyValuePairOrBuilder getMetaOrBuilder(
        int index);

    /**
     * <code>optional .Units units = 5;</code>
     * @return Whether the units field is set.
     */
    boolean hasUnits();
    /**
     * <code>optional .Units units = 5;</code>
     * @return The enum numeric value on the wire for units.
     */
    int getUnitsValue();
    /**
     * <code>optional .Units units = 5;</code>
     * @return The units.
     */
    NightLiteProto.Units getUnits();
  }
  /**
   * Protobuf type {@code Block}
   */
  public static final class Block extends
      com.google.protobuf.GeneratedMessage implements
      // @@protoc_insertion_point(message_implements:Block)
      BlockOrBuilder {
  private static final long serialVersionUID = 0L;
    static {
      com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(
        com.google.protobuf.RuntimeVersion.RuntimeDomain.PUBLIC,
        /* major= */ 4,
        /* minor= */ 34,
        /* patch= */ 0,
        /* suffix= */ "",
        "Block");
    }
    // Use Block.newBuilder() to construct.
    private Block(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
      super(builder);
    }
    private Block() {
      entries_ = java.util.Collections.emptyList();
      treatments_ = java.util.Collections.emptyList();
      special_ = java.util.Collections.emptyList();
      meta_ = java.util.Collections.emptyList();
      units_ = 0;
    }

    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return NightLiteProto.internal_static_Block_descriptor;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return NightLiteProto.internal_static_Block_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return NightLiteProto.internal_static_Block_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              NightLiteProto.Block.class, NightLiteProto.Block.Builder.class);
    }

    private int bitField0_;
    public static final int ENTRIES_FIELD_NUMBER = 1;
    @SuppressWarnings("serial")
    private java.util.List<NightLiteProto.GlucoseEntry> entries_;
    /**
     * <code>repeated .GlucoseEntry entries = 1;</code>
     */
    @java.lang.Override
    public java.util.List<NightLiteProto.GlucoseEntry> getEntriesList() {
      return entries_;
    }
    /**
     * <code>repeated .GlucoseEntry entries = 1;</code>
     */
    @java.lang.Override
    public java.util.List<? extends NightLiteProto.GlucoseEntryOrBuilder> 
        getEntriesOrBuilderList() {
      return entries_;
    }
    /**
     * <code>repeated .GlucoseEntry entries = 1;</code>
     */
    @java.lang.Override
    public int getEntriesCount() {
      return entries_.size();
    }
    /**
     * <code>repeated .GlucoseEntry entries = 1;</code>
     */
    @java.lang.Override
    public NightLiteProto.GlucoseEntry getEntries(int index) {
      return entries_.get(index);
    }
    /**
     * <code>repeated .GlucoseEntry entries = 1;</code>
     */
    @java.lang.Override
    public NightLiteProto.GlucoseEntryOrBuilder getEntriesOrBuilder(
        int index) {
      return entries_.get(index);
    }

    public static final int TREATMENTS_FIELD_NUMBER = 2;
    @SuppressWarnings("serial")
    private java.util.List<NightLiteProto.Treatment> treatments_;
    /**
     * <code>repeated .Treatment treatments = 2;</code>
     */
    @java.lang.Override
    public java.util.List<NightLiteProto.Treatment> getTreatmentsList() {
      return treatments_;
    }
    /**
     * <code>repeated .Treatment treatments = 2;</code>
     */
    @java.lang.Override
    public java.util.List<? extends NightLiteProto.TreatmentOrBuilder> 
        getTreatmentsOrBuilderList() {
      return treatments_;
    }
    /**
     * <code>repeated .Treatment treatments = 2;</code>
     */
    @java.lang.Override
    public int getTreatmentsCount() {
      return treatments_.size();
    }
    /**
     * <code>repeated .Treatment treatments = 2;</code>
     */
    @java.lang.Override
    public NightLiteProto.Treatment getTreatments(int index) {
      return treatments_.get(index);
    }
    /**
     * <code>repeated .Treatment treatments = 2;</code>
     */
    @java.lang.Override
    public NightLiteProto.TreatmentOrBuilder getTreatmentsOrBuilder(
        int index) {
      return treatments_.get(index);
    }

    public static final int SPECIAL_FIELD_NUMBER = 3;
    @SuppressWarnings("serial")
    private java.util.List<NightLiteProto.Special> special_;
    /**
     * <code>repeated .Special special = 3;</code>
     */
    @java.lang.Override
    public java.util.List<NightLiteProto.Special> getSpecialList() {
      return special_;
    }
    /**
     * <code>repeated .Special special = 3;</code>
     */
    @java.lang.Override
    public java.util.List<? extends NightLiteProto.SpecialOrBuilder> 
        getSpecialOrBuilderList() {
      return special_;
    }
    /**
     * <code>repeated .Special special = 3;</code>
     */
    @java.lang.Override
    public int getSpecialCount() {
      return special_.size();
    }
    /**
     * <code>repeated .Special special = 3;</code>
     */
    @java.lang.Override
    public NightLiteProto.Special getSpecial(int index) {
      return special_.get(index);
    }
    /**
     * <code>repeated .Special special = 3;</code>
     */
    @java.lang.Override
    public NightLiteProto.SpecialOrBuilder getSpecialOrBuilder(
        int index) {
      return special_.get(index);
    }

    public static final int META_FIELD_NUMBER = 4;
    @SuppressWarnings("serial")
    private java.util.List<NightLiteProto.KeyValuePair> meta_;
    /**
     * <code>repeated .KeyValuePair meta = 4;</code>
     */
    @java.lang.Override
    public java.util.List<NightLiteProto.KeyValuePair> getMetaList() {
      return meta_;
    }
    /**
     * <code>repeated .KeyValuePair meta = 4;</code>
     */
    @java.lang.Override
    public java.util.List<? extends NightLiteProto.KeyValuePairOrBuilder> 
        getMetaOrBuilderList() {
      return meta_;
    }
    /**
     * <code>repeated .KeyValuePair meta = 4;</code>
     */
    @java.lang.Override
    public int getMetaCount() {
      return meta_.size();
    }
    /**
     * <code>repeated .KeyValuePair meta = 4;</code>
     */
    @java.lang.Override
    public NightLiteProto.KeyValuePair getMeta(int index) {
      return meta_.get(index);
    }
    /**
     * <code>repeated .KeyValuePair meta = 4;</code>
     */
    @java.lang.Override
    public NightLiteProto.KeyValuePairOrBuilder getMetaOrBuilder(
        int index) {
      return meta_.get(index);
    }

    public static final int UNITS_FIELD_NUMBER = 5;
    private int units_ = 0;
    /**
     * <code>optional .Units units = 5;</code>
     * @return Whether the units field is set.
     */
    @java.lang.Override public boolean hasUnits() {
      return ((bitField0_ & 0x00000001) != 0);
    }
    /**
     * <code>optional .Units units = 5;</code>
     * @return The enum numeric value on the wire for units.
     */
    @java.lang.Override public int getUnitsValue() {
      return units_;
    }
    /**
     * <code>optional .Units units = 5;</code>
     * @return The units.
     */
    @java.lang.Override public NightLiteProto.Units getUnits() {
      NightLiteProto.Units result = NightLiteProto.Units.forNumber(units_);
      return result == null ? NightLiteProto.Units.UNRECOGNIZED : result;
    }

    private byte memoizedIsInitialized = -1;
    @java.lang.Override
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      memoizedIsInitialized = 1;
      return true;
    }

    @java.lang.Override
    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      for (int i = 0; i < entries_.size(); i++) {
        output.writeMessage(1, entries_.get(i));
      }
      for (int i = 0; i < treatments_.size(); i++) {
        output.writeMessage(2, treatments_.get(i));
      }
      for (int i = 0; i < special_.size(); i++) {
        output.writeMessage(3, special_.get(i));
      }
      for (int i = 0; i < meta_.size(); i++) {
        output.writeMessage(4, meta_.get(i));
      }
      if (((bitField0_ & 0x00000001) != 0)) {
        output.writeEnum(5, units_);
      }
      getUnknownFields().writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;

          {
            final int count = entries_.size();
            for (int i = 0; i < count; i++) {
              size += com.google.protobuf.CodedOutputStream
                .computeMessageSizeNoTag(entries_.get(i));
            }
            size += 1 * count;
          }

          {
            final int count = treatments_.size();
            for (int i = 0; i < count; i++) {
              size += com.google.protobuf.CodedOutputStream
                .computeMessageSizeNoTag(treatments_.get(i));
            }
            size += 1 * count;
          }

          {
            final int count = special_.size();
            for (int i = 0; i < count; i++) {
              size += com.google.protobuf.CodedOutputStream
                .computeMessageSizeNoTag(special_.get(i));
            }
            size += 1 * count;
          }

          {
            final int count = meta_.size();
            for (int i = 0; i < count; i++) {
              size += com.google.protobuf.CodedOutputStream
                .computeMessageSizeNoTag(meta_.get(i));
            }
            size += 1 * count;
          }
      if (((bitField0_ & 0x00000001) != 0)) {
        size += com.google.protobuf.CodedOutputStream
          .computeEnumSize(5, units_);
      }
      size += getUnknownFields().getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object obj) {
      if (obj == this) {
       return true;
      }
      if (!(obj instanceof NightLiteProto.Block)) {
        return super.equals(obj);
      }
      NightLiteProto.Block other = (NightLiteProto.Block) obj;

      if (!getEntriesList()
          .equals(other.getEntriesList())) return false;
      if (!getTreatmentsList()
          .equals(other.getTreatmentsList())) return false;
      if (!getSpecialList()
          .equals(other.getSpecialList())) return false;
      if (!getMetaList()
          .equals(other.getMetaList())) return false;
      if (hasUnits() != other.hasUnits()) return false;
      if (hasUnits()) {
        if (units_ != other.units_) return false;
      }
      if (!getUnknownFields().equals(other.getUnknownFields())) return false;
      return true;
    }

    @java.lang.Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      if (getEntriesCount() > 0) {
        hash = (37 * hash) + ENTRIES_FIELD_NUMBER;
        hash = (53 * hash) + getEntriesList().hashCode();
      }
      if (getTreatmentsCount() > 0) {
        hash = (37 * hash) + TREATMENTS_FIELD_NUMBER;
        hash = (53 * hash) + getTreatmentsList().hashCode();
      }
      if (getSpecialCount() > 0) {
        hash = (37 * hash) + SPECIAL_FIELD_NUMBER;
        hash = (53 * hash) + getSpecialList().hashCode();
      }
      if (getMetaCount() > 0) {
        hash = (37 * hash) + META_FIELD_NUMBER;
        hash = (53 * hash) + getMetaList().hashCode();
      }
      if (hasUnits()) {
        hash = (37 * hash) + UNITS_FIELD_NUMBER;
        hash = (53 * hash) + units_;
      }
      hash = (29 * hash) + getUnknownFields().hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static NightLiteProto.Block parseFrom(
        java.nio.ByteBuffer data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static NightLiteProto.Block parseFrom(
        java.nio.ByteBuffer data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static NightLiteProto.Block parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static NightLiteProto.Block parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static NightLiteProto.Block parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static NightLiteProto.Block parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static NightLiteProto.Block parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseWithIOException(PARSER, input);
    }
    public static NightLiteProto.Block parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static NightLiteProto.Block parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseDelimitedWithIOException(PARSER, input);
    }

    public static NightLiteProto.Block parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }
    public static NightLiteProto.Block parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseWithIOException(PARSER, input);
    }
    public static NightLiteProto.Block parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }
    public static Builder newBuilder(NightLiteProto.Block prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }
    @java.lang.Override
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE
          ? new Builder() : new Builder().mergeFrom(this);
    }

    @java.lang.Override
    protected Builder newBuilderForType(
        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }
    /**
     * Protobuf type {@code Block}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:Block)
        NightLiteProto.BlockOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return NightLiteProto.internal_static_Block_descriptor;
      }

      @java.lang.Override
      protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return NightLiteProto.internal_static_Block_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                NightLiteProto.Block.class, NightLiteProto.Block.Builder.class);
      }

      // Construct using NightLiteProto.Block.newBuilder()
      private Builder() {

      }

      private Builder(
          com.google.protobuf.GeneratedMessage.BuilderParent parent) {
        super(parent);

      }
      @java.lang.Override
      public Builder clear() {
        super.clear();
        bitField0_ = 0;
        if (entriesBuilder_ == null) {
          entries_ = java.util.Collections.emptyList();
        } else {
          entries_ = null;
          entriesBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000001);
        if (treatmentsBuilder_ == null) {
          treatments_ = java.util.Collections.emptyList();
        } else {
          treatments_ = null;
          treatmentsBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000002);
        if (specialBuilder_ == null) {
          special_ = java.util.Collections.emptyList();
        } else {
          special_ = null;
          specialBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000004);
        if (metaBuilder_ == null) {
          meta_ = java.util.Collections.emptyList();
        } else {
          meta_ = null;
          metaBuilder_.clear();
        }
        bitField0_ = (bitField0_ & ~0x00000008);
        units_ = 0;
        return this;
      }

      @java.lang.Override
      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return NightLiteProto.internal_static_Block_descriptor;
      }

      @java.lang.Override
      public NightLiteProto.Block getDefaultInstanceForType() {
        return NightLiteProto.Block.getDefaultInstance();
      }

      @java.lang.Override
      public NightLiteProto.Block build() {
        NightLiteProto.Block result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      @java.lang.Override
      public NightLiteProto.Block buildPartial() {
        NightLiteProto.Block result = new NightLiteProto.Block(this);
        buildPartialRepeatedFields(result);
        if (bitField0_ != 0) { buildPartial0(result); }
        onBuilt();
        return result;
      }

      private void buildPartialRepeatedFields(NightLiteProto.Block result) {
        if (entriesBuilder_ == null) {
          if (((bitField0_ & 0x00000001) != 0)) {
            entries_ = java.util.Collections.unmodifiableList(entries_);
            bitField0_ = (bitField0_ & ~0x00000001);
          }
          result.entries_ = entries_;
        } else {
          result.entries_ = entriesBuilder_.build();
        }
        if (treatmentsBuilder_ == null) {
          if (((bitField0_ & 0x00000002) != 0)) {
            treatments_ = java.util.Collections.unmodifiableList(treatments_);
            bitField0_ = (bitField0_ & ~0x00000002);
          }
          result.treatments_ = treatments_;
        } else {
          result.treatments_ = treatmentsBuilder_.build();
        }
        if (specialBuilder_ == null) {
          if (((bitField0_ & 0x00000004) != 0)) {
            special_ = java.util.Collections.unmodifiableList(special_);
            bitField0_ = (bitField0_ & ~0x00000004);
          }
          result.special_ = special_;
        } else {
          result.special_ = specialBuilder_.build();
        }
        if (metaBuilder_ == null) {
          if (((bitField0_ & 0x00000008) != 0)) {
            meta_ = java.util.Collections.unmodifiableList(meta_);
            bitField0_ = (bitField0_ & ~0x00000008);
          }
          result.meta_ = meta_;
        } else {
          result.meta_ = metaBuilder_.build();
        }
      }

      private void buildPartial0(NightLiteProto.Block result) {
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000010) != 0)) {
          result.units_ = units_;
          to_bitField0_ |= 0x00000001;
        }
        result.bitField0_ |= to_bitField0_;
      }

      @java.lang.Override
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof NightLiteProto.Block) {
          return mergeFrom((NightLiteProto.Block)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(NightLiteProto.Block other) {
        if (other == NightLiteProto.Block.getDefaultInstance()) return this;
        if (entriesBuilder_ == null) {
          if (!other.entries_.isEmpty()) {
            if (entries_.isEmpty()) {
              entries_ = other.entries_;
              bitField0_ = (bitField0_ & ~0x00000001);
            } else {
              ensureEntriesIsMutable();
              entries_.addAll(other.entries_);
            }
            onChanged();
          }
        } else {
          if (!other.entries_.isEmpty()) {
            if (entriesBuilder_.isEmpty()) {
              entriesBuilder_.dispose();
              entriesBuilder_ = null;
              entries_ = other.entries_;
              bitField0_ = (bitField0_ & ~0x00000001);
              entriesBuilder_ = 
                com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders ?
                   internalGetEntriesFieldBuilder() : null;
            } else {
              entriesBuilder_.addAllMessages(other.entries_);
            }
          }
        }
        if (treatmentsBuilder_ == null) {
          if (!other.treatments_.isEmpty()) {
            if (treatments_.isEmpty()) {
              treatments_ = other.treatments_;
              bitField0_ = (bitField0_ & ~0x00000002);
            } else {
              ensureTreatmentsIsMutable();
              treatments_.addAll(other.treatments_);
            }
            onChanged();
          }
        } else {
          if (!other.treatments_.isEmpty()) {
            if (treatmentsBuilder_.isEmpty()) {
              treatmentsBuilder_.dispose();
              treatmentsBuilder_ = null;
              treatments_ = other.treatments_;
              bitField0_ = (bitField0_ & ~0x00000002);
              treatmentsBuilder_ = 
                com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders ?
                   internalGetTreatmentsFieldBuilder() : null;
            } else {
              treatmentsBuilder_.addAllMessages(other.treatments_);
            }
          }
        }
        if (specialBuilder_ == null) {
          if (!other.special_.isEmpty()) {
            if (special_.isEmpty()) {
              special_ = other.special_;
              bitField0_ = (bitField0_ & ~0x00000004);
            } else {
              ensureSpecialIsMutable();
              special_.addAll(other.special_);
            }
            onChanged();
          }
        } else {
          if (!other.special_.isEmpty()) {
            if (specialBuilder_.isEmpty()) {
              specialBuilder_.dispose();
              specialBuilder_ = null;
              special_ = other.special_;
              bitField0_ = (bitField0_ & ~0x00000004);
              specialBuilder_ = 
                com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders ?
                   internalGetSpecialFieldBuilder() : null;
            } else {
              specialBuilder_.addAllMessages(other.special_);
            }
          }
        }
        if (metaBuilder_ == null) {
          if (!other.meta_.isEmpty()) {
            if (meta_.isEmpty()) {
              meta_ = other.meta_;
              bitField0_ = (bitField0_ & ~0x00000008);
            } else {
              ensureMetaIsMutable();
              meta_.addAll(other.meta_);
            }
            onChanged();
          }
        } else {
          if (!other.meta_.isEmpty()) {
            if (metaBuilder_.isEmpty()) {
              metaBuilder_.dispose();
              metaBuilder_ = null;
              meta_ = other.meta_;
              bitField0_ = (bitField0_ & ~0x00000008);
              metaBuilder_ = 
                com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders ?
                   internalGetMetaFieldBuilder() : null;
            } else {
              metaBuilder_.addAllMessages(other.meta_);
            }
          }
        }
        if (other.hasUnits()) {
          setUnitsValue(other.getUnitsValue());
        }
        this.mergeUnknownFields(other.getUnknownFields());
        onChanged();
        return this;
      }

      @java.lang.Override
      public final boolean isInitialized() {
        return true;
      }

      @java.lang.Override
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        if (extensionRegistry == null) {
          throw new java.lang.NullPointerException();
        }
        try {
          boolean done = false;
          while (!done) {
            int tag = input.readTag();
            switch (tag) {
              case 0:
                done = true;
                break;
              case 10: {
                NightLiteProto.GlucoseEntry m =
                    input.readMessage(
                        NightLiteProto.GlucoseEntry.parser(),
                        extensionRegistry);
                if (entriesBuilder_ == null) {
                  ensureEntriesIsMutable();
                  entries_.add(m);
                } else {
                  entriesBuilder_.addMessage(m);
                }
                break;
              } // case 10
              case 18: {
                NightLiteProto.Treatment m =
                    input.readMessage(
                        NightLiteProto.Treatment.parser(),
                        extensionRegistry);
                if (treatmentsBuilder_ == null) {
                  ensureTreatmentsIsMutable();
                  treatments_.add(m);
                } else {
                  treatmentsBuilder_.addMessage(m);
                }
                break;
              } // case 18
              case 26: {
                NightLiteProto.Special m =
                    input.readMessage(
                        NightLiteProto.Special.parser(),
                        extensionRegistry);
                if (specialBuilder_ == null) {
                  ensureSpecialIsMutable();
                  special_.add(m);
                } else {
                  specialBuilder_.addMessage(m);
                }
                break;
              } // case 26
              case 34: {
                NightLiteProto.KeyValuePair m =
                    input.readMessage(
                        NightLiteProto.KeyValuePair.parser(),
                        extensionRegistry);
                if (metaBuilder_ == null) {
                  ensureMetaIsMutable();
                  meta_.add(m);
                } else {
                  metaBuilder_.addMessage(m);
                }
                break;
              } // case 34
              case 40: {
                units_ = input.readEnum();
                bitField0_ |= 0x00000010;
                break;
              } // case 40
              default: {
                if (!super.parseUnknownField(input, extensionRegistry, tag)) {
                  done = true; // was an endgroup tag
                }
                break;
              } // default:
            } // switch (tag)
          } // while (!done)
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          throw e.unwrapIOException();
        } finally {
          onChanged();
        } // finally
        return this;
      }
      private int bitField0_;

      private java.util.List<NightLiteProto.GlucoseEntry> entries_ =
        java.util.Collections.emptyList();
      private void ensureEntriesIsMutable() {
        if (!((bitField0_ & 0x00000001) != 0)) {
          entries_ = new java.util.ArrayList<NightLiteProto.GlucoseEntry>(entries_);
          bitField0_ |= 0x00000001;
         }
      }

      private com.google.protobuf.RepeatedFieldBuilder<
          NightLiteProto.GlucoseEntry, NightLiteProto.GlucoseEntry.Builder, NightLiteProto.GlucoseEntryOrBuilder> entriesBuilder_;

      /**
       * <code>repeated .GlucoseEntry entries = 1;</code>
       */
      public java.util.List<NightLiteProto.GlucoseEntry> getEntriesList() {
        if (entriesBuilder_ == null) {
          return java.util.Collections.unmodifiableList(entries_);
        } else {
          return entriesBuilder_.getMessageList();
        }
      }
      /**
       * <code>repeated .GlucoseEntry entries = 1;</code>
       */
      public int getEntriesCount() {
        if (entriesBuilder_ == null) {
          return entries_.size();
        } else {
          return entriesBuilder_.getCount();
        }
      }
      /**
       * <code>repeated .GlucoseEntry entries = 1;</code>
       */
      public NightLiteProto.GlucoseEntry getEntries(int index) {
        if (entriesBuilder_ == null) {
          return entries_.get(index);
        } else {
          return entriesBuilder_.getMessage(index);
        }
      }
      /**
       * <code>repeated .GlucoseEntry entries = 1;</code>
       */
      public Builder setEntries(
          int index, NightLiteProto.GlucoseEntry value) {
        if (entriesBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureEntriesIsMutable();
          entries_.set(index, value);
          onChanged();
        } else {
          entriesBuilder_.setMessage(index, value);
        }
        return this;
      }
      /**
       * <code>repeated .GlucoseEntry entries = 1;</code>
       */
      public Builder setEntries(
          int index, NightLiteProto.GlucoseEntry.Builder builderForValue) {
        if (entriesBuilder_ == null) {
          ensureEntriesIsMutable();
          entries_.set(index, builderForValue.build());
          onChanged();
        } else {
          entriesBuilder_.setMessage(index, builderForValue.build());
        }
        return this;
      }
      /**
       * <code>repeated .GlucoseEntry entries = 1;</code>
       */
      public Builder addEntries(NightLiteProto.GlucoseEntry value) {
        if (entriesBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureEntriesIsMutable();
          entries_.add(value);
          onChanged();
        } else {
          entriesBuilder_.addMessage(value);
        }
        return this;
      }
      /**
       * <code>repeated .GlucoseEntry entries = 1;</code>
       */
      public Builder addEntries(
          int index, NightLiteProto.GlucoseEntry value) {
        if (entriesBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureEntriesIsMutable();
          entries_.add(index, value);
          onChanged();
        } else {
          entriesBuilder_.addMessage(index, value);
        }
        return this;
      }
      /**
       * <code>repeated .GlucoseEntry entries = 1;</code>
       */
      public Builder addEntries(
          NightLiteProto.GlucoseEntry.Builder builderForValue) {
        if (entriesBuilder_ == null) {
          ensureEntriesIsMutable();
          entries_.add(builderForValue.build());
          onChanged();
        } else {
          entriesBuilder_.addMessage(builderForValue.build());
        }
        return this;
      }
      /**
       * <code>repeated .GlucoseEntry entries = 1;</code>
       */
      public Builder addEntries(
          int index, NightLiteProto.GlucoseEntry.Builder builderForValue) {
        if (entriesBuilder_ == null) {
          ensureEntriesIsMutable();
          entries_.add(index, builderForValue.build());
          onChanged();
        } else {
          entriesBuilder_.addMessage(index, builderForValue.build());
        }
        return this;
      }
      /**
       * <code>repeated .GlucoseEntry entries = 1;</code>
       */
      public Builder addAllEntries(
          java.lang.Iterable<? extends NightLiteProto.GlucoseEntry> values) {
        if (entriesBuilder_ == null) {
          ensureEntriesIsMutable();
          com.google.protobuf.AbstractMessageLite.Builder.addAll(
              values, entries_);
          onChanged();
        } else {
          entriesBuilder_.addAllMessages(values);
        }
        return this;
      }
      /**
       * <code>repeated .GlucoseEntry entries = 1;</code>
       */
      public Builder clearEntries() {
        if (entriesBuilder_ == null) {
          entries_ = java.util.Collections.emptyList();
          bitField0_ = (bitField0_ & ~0x00000001);
          onChanged();
        } else {
          entriesBuilder_.clear();
        }
        return this;
      }
      /**
       * <code>repeated .GlucoseEntry entries = 1;</code>
       */
      public Builder removeEntries(int index) {
        if (entriesBuilder_ == null) {
          ensureEntriesIsMutable();
          entries_.remove(index);
          onChanged();
        } else {
          entriesBuilder_.remove(index);
        }
        return this;
      }
      /**
       * <code>repeated .GlucoseEntry entries = 1;</code>
       */
      public NightLiteProto.GlucoseEntry.Builder getEntriesBuilder(
          int index) {
        return internalGetEntriesFieldBuilder().getBuilder(index);
      }
      /**
       * <code>repeated .GlucoseEntry entries = 1;</code>
       */
      public NightLiteProto.GlucoseEntryOrBuilder getEntriesOrBuilder(
          int index) {
        if (entriesBuilder_ == null) {
          return entries_.get(index);  } else {
          return entriesBuilder_.getMessageOrBuilder(index);
        }
      }
      /**
       * <code>repeated .GlucoseEntry entries = 1;</code>
       */
      public java.util.List<? extends NightLiteProto.GlucoseEntryOrBuilder> 
           getEntriesOrBuilderList() {
        if (entriesBuilder_ != null) {
          return entriesBuilder_.getMessageOrBuilderList();
        } else {
          return java.util.Collections.unmodifiableList(entries_);
        }
      }
      /**
       * <code>repeated .GlucoseEntry entries = 1;</code>
       */
      public NightLiteProto.GlucoseEntry.Builder addEntriesBuilder() {
        return internalGetEntriesFieldBuilder().addBuilder(
            NightLiteProto.GlucoseEntry.getDefaultInstance());
      }
      /**
       * <code>repeated .GlucoseEntry entries = 1;</code>
       */
      public NightLiteProto.GlucoseEntry.Builder addEntriesBuilder(
          int index) {
        return internalGetEntriesFieldBuilder().addBuilder(
            index, NightLiteProto.GlucoseEntry.getDefaultInstance());
      }
      /**
       * <code>repeated .GlucoseEntry entries = 1;</code>
       */
      public java.util.List<NightLiteProto.GlucoseEntry.Builder> 
           getEntriesBuilderList() {
        return internalGetEntriesFieldBuilder().getBuilderList();
      }
      private com.google.protobuf.RepeatedFieldBuilder<
          NightLiteProto.GlucoseEntry, NightLiteProto.GlucoseEntry.Builder, NightLiteProto.GlucoseEntryOrBuilder> 
          internalGetEntriesFieldBuilder() {
        if (entriesBuilder_ == null) {
          entriesBuilder_ = new com.google.protobuf.RepeatedFieldBuilder<
              NightLiteProto.GlucoseEntry, NightLiteProto.GlucoseEntry.Builder, NightLiteProto.GlucoseEntryOrBuilder>(
                  entries_,
                  ((bitField0_ & 0x00000001) != 0),
                  getParentForChildren(),
                  isClean());
          entries_ = null;
        }
        return entriesBuilder_;
      }

      private java.util.List<NightLiteProto.Treatment> treatments_ =
        java.util.Collections.emptyList();
      private void ensureTreatmentsIsMutable() {
        if (!((bitField0_ & 0x00000002) != 0)) {
          treatments_ = new java.util.ArrayList<NightLiteProto.Treatment>(treatments_);
          bitField0_ |= 0x00000002;
         }
      }

      private com.google.protobuf.RepeatedFieldBuilder<
          NightLiteProto.Treatment, NightLiteProto.Treatment.Builder, NightLiteProto.TreatmentOrBuilder> treatmentsBuilder_;

      /**
       * <code>repeated .Treatment treatments = 2;</code>
       */
      public java.util.List<NightLiteProto.Treatment> getTreatmentsList() {
        if (treatmentsBuilder_ == null) {
          return java.util.Collections.unmodifiableList(treatments_);
        } else {
          return treatmentsBuilder_.getMessageList();
        }
      }
      /**
       * <code>repeated .Treatment treatments = 2;</code>
       */
      public int getTreatmentsCount() {
        if (treatmentsBuilder_ == null) {
          return treatments_.size();
        } else {
          return treatmentsBuilder_.getCount();
        }
      }
      /**
       * <code>repeated .Treatment treatments = 2;</code>
       */
      public NightLiteProto.Treatment getTreatments(int index) {
        if (treatmentsBuilder_ == null) {
          return treatments_.get(index);
        } else {
          return treatmentsBuilder_.getMessage(index);
        }
      }
      /**
       * <code>repeated .Treatment treatments = 2;</code>
       */
      public Builder setTreatments(
          int index, NightLiteProto.Treatment value) {
        if (treatmentsBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureTreatmentsIsMutable();
          treatments_.set(index, value);
          onChanged();
        } else {
          treatmentsBuilder_.setMessage(index, value);
        }
        return this;
      }
      /**
       * <code>repeated .Treatment treatments = 2;</code>
       */
      public Builder setTreatments(
          int index, NightLiteProto.Treatment.Builder builderForValue) {
        if (treatmentsBuilder_ == null) {
          ensureTreatmentsIsMutable();
          treatments_.set(index, builderForValue.build());
          onChanged();
        } else {
          treatmentsBuilder_.setMessage(index, builderForValue.build());
        }
        return this;
      }
      /**
       * <code>repeated .Treatment treatments = 2;</code>
       */
      public Builder addTreatments(NightLiteProto.Treatment value) {
        if (treatmentsBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureTreatmentsIsMutable();
          treatments_.add(value);
          onChanged();
        } else {
          treatmentsBuilder_.addMessage(value);
        }
        return this;
      }
      /**
       * <code>repeated .Treatment treatments = 2;</code>
       */
      public Builder addTreatments(
          int index, NightLiteProto.Treatment value) {
        if (treatmentsBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureTreatmentsIsMutable();
          treatments_.add(index, value);
          onChanged();
        } else {
          treatmentsBuilder_.addMessage(index, value);
        }
        return this;
      }
      /**
       * <code>repeated .Treatment treatments = 2;</code>
       */
      public Builder addTreatments(
          NightLiteProto.Treatment.Builder builderForValue) {
        if (treatmentsBuilder_ == null) {
          ensureTreatmentsIsMutable();
          treatments_.add(builderForValue.build());
          onChanged();
        } else {
          treatmentsBuilder_.addMessage(builderForValue.build());
        }
        return this;
      }
      /**
       * <code>repeated .Treatment treatments = 2;</code>
       */
      public Builder addTreatments(
          int index, NightLiteProto.Treatment.Builder builderForValue) {
        if (treatmentsBuilder_ == null) {
          ensureTreatmentsIsMutable();
          treatments_.add(index, builderForValue.build());
          onChanged();
        } else {
          treatmentsBuilder_.addMessage(index, builderForValue.build());
        }
        return this;
      }
      /**
       * <code>repeated .Treatment treatments = 2;</code>
       */
      public Builder addAllTreatments(
          java.lang.Iterable<? extends NightLiteProto.Treatment> values) {
        if (treatmentsBuilder_ == null) {
          ensureTreatmentsIsMutable();
          com.google.protobuf.AbstractMessageLite.Builder.addAll(
              values, treatments_);
          onChanged();
        } else {
          treatmentsBuilder_.addAllMessages(values);
        }
        return this;
      }
      /**
       * <code>repeated .Treatment treatments = 2;</code>
       */
      public Builder clearTreatments() {
        if (treatmentsBuilder_ == null) {
          treatments_ = java.util.Collections.emptyList();
          bitField0_ = (bitField0_ & ~0x00000002);
          onChanged();
        } else {
          treatmentsBuilder_.clear();
        }
        return this;
      }
      /**
       * <code>repeated .Treatment treatments = 2;</code>
       */
      public Builder removeTreatments(int index) {
        if (treatmentsBuilder_ == null) {
          ensureTreatmentsIsMutable();
          treatments_.remove(index);
          onChanged();
        } else {
          treatmentsBuilder_.remove(index);
        }
        return this;
      }
      /**
       * <code>repeated .Treatment treatments = 2;</code>
       */
      public NightLiteProto.Treatment.Builder getTreatmentsBuilder(
          int index) {
        return internalGetTreatmentsFieldBuilder().getBuilder(index);
      }
      /**
       * <code>repeated .Treatment treatments = 2;</code>
       */
      public NightLiteProto.TreatmentOrBuilder getTreatmentsOrBuilder(
          int index) {
        if (treatmentsBuilder_ == null) {
          return treatments_.get(index);  } else {
          return treatmentsBuilder_.getMessageOrBuilder(index);
        }
      }
      /**
       * <code>repeated .Treatment treatments = 2;</code>
       */
      public java.util.List<? extends NightLiteProto.TreatmentOrBuilder> 
           getTreatmentsOrBuilderList() {
        if (treatmentsBuilder_ != null) {
          return treatmentsBuilder_.getMessageOrBuilderList();
        } else {
          return java.util.Collections.unmodifiableList(treatments_);
        }
      }
      /**
       * <code>repeated .Treatment treatments = 2;</code>
       */
      public NightLiteProto.Treatment.Builder addTreatmentsBuilder() {
        return internalGetTreatmentsFieldBuilder().addBuilder(
            NightLiteProto.Treatment.getDefaultInstance());
      }
      /**
       * <code>repeated .Treatment treatments = 2;</code>
       */
      public NightLiteProto.Treatment.Builder addTreatmentsBuilder(
          int index) {
        return internalGetTreatmentsFieldBuilder().addBuilder(
            index, NightLiteProto.Treatment.getDefaultInstance());
      }
      /**
       * <code>repeated .Treatment treatments = 2;</code>
       */
      public java.util.List<NightLiteProto.Treatment.Builder> 
           getTreatmentsBuilderList() {
        return internalGetTreatmentsFieldBuilder().getBuilderList();
      }
      private com.google.protobuf.RepeatedFieldBuilder<
          NightLiteProto.Treatment, NightLiteProto.Treatment.Builder, NightLiteProto.TreatmentOrBuilder> 
          internalGetTreatmentsFieldBuilder() {
        if (treatmentsBuilder_ == null) {
          treatmentsBuilder_ = new com.google.protobuf.RepeatedFieldBuilder<
              NightLiteProto.Treatment, NightLiteProto.Treatment.Builder, NightLiteProto.TreatmentOrBuilder>(
                  treatments_,
                  ((bitField0_ & 0x00000002) != 0),
                  getParentForChildren(),
                  isClean());
          treatments_ = null;
        }
        return treatmentsBuilder_;
      }

      private java.util.List<NightLiteProto.Special> special_ =
        java.util.Collections.emptyList();
      private void ensureSpecialIsMutable() {
        if (!((bitField0_ & 0x00000004) != 0)) {
          special_ = new java.util.ArrayList<NightLiteProto.Special>(special_);
          bitField0_ |= 0x00000004;
         }
      }

      private com.google.protobuf.RepeatedFieldBuilder<
          NightLiteProto.Special, NightLiteProto.Special.Builder, NightLiteProto.SpecialOrBuilder> specialBuilder_;

      /**
       * <code>repeated .Special special = 3;</code>
       */
      public java.util.List<NightLiteProto.Special> getSpecialList() {
        if (specialBuilder_ == null) {
          return java.util.Collections.unmodifiableList(special_);
        } else {
          return specialBuilder_.getMessageList();
        }
      }
      /**
       * <code>repeated .Special special = 3;</code>
       */
      public int getSpecialCount() {
        if (specialBuilder_ == null) {
          return special_.size();
        } else {
          return specialBuilder_.getCount();
        }
      }
      /**
       * <code>repeated .Special special = 3;</code>
       */
      public NightLiteProto.Special getSpecial(int index) {
        if (specialBuilder_ == null) {
          return special_.get(index);
        } else {
          return specialBuilder_.getMessage(index);
        }
      }
      /**
       * <code>repeated .Special special = 3;</code>
       */
      public Builder setSpecial(
          int index, NightLiteProto.Special value) {
        if (specialBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureSpecialIsMutable();
          special_.set(index, value);
          onChanged();
        } else {
          specialBuilder_.setMessage(index, value);
        }
        return this;
      }
      /**
       * <code>repeated .Special special = 3;</code>
       */
      public Builder setSpecial(
          int index, NightLiteProto.Special.Builder builderForValue) {
        if (specialBuilder_ == null) {
          ensureSpecialIsMutable();
          special_.set(index, builderForValue.build());
          onChanged();
        } else {
          specialBuilder_.setMessage(index, builderForValue.build());
        }
        return this;
      }
      /**
       * <code>repeated .Special special = 3;</code>
       */
      public Builder addSpecial(NightLiteProto.Special value) {
        if (specialBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureSpecialIsMutable();
          special_.add(value);
          onChanged();
        } else {
          specialBuilder_.addMessage(value);
        }
        return this;
      }
      /**
       * <code>repeated .Special special = 3;</code>
       */
      public Builder addSpecial(
          int index, NightLiteProto.Special value) {
        if (specialBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureSpecialIsMutable();
          special_.add(index, value);
          onChanged();
        } else {
          specialBuilder_.addMessage(index, value);
        }
        return this;
      }
      /**
       * <code>repeated .Special special = 3;</code>
       */
      public Builder addSpecial(
          NightLiteProto.Special.Builder builderForValue) {
        if (specialBuilder_ == null) {
          ensureSpecialIsMutable();
          special_.add(builderForValue.build());
          onChanged();
        } else {
          specialBuilder_.addMessage(builderForValue.build());
        }
        return this;
      }
      /**
       * <code>repeated .Special special = 3;</code>
       */
      public Builder addSpecial(
          int index, NightLiteProto.Special.Builder builderForValue) {
        if (specialBuilder_ == null) {
          ensureSpecialIsMutable();
          special_.add(index, builderForValue.build());
          onChanged();
        } else {
          specialBuilder_.addMessage(index, builderForValue.build());
        }
        return this;
      }
      /**
       * <code>repeated .Special special = 3;</code>
       */
      public Builder addAllSpecial(
          java.lang.Iterable<? extends NightLiteProto.Special> values) {
        if (specialBuilder_ == null) {
          ensureSpecialIsMutable();
          com.google.protobuf.AbstractMessageLite.Builder.addAll(
              values, special_);
          onChanged();
        } else {
          specialBuilder_.addAllMessages(values);
        }
        return this;
      }
      /**
       * <code>repeated .Special special = 3;</code>
       */
      public Builder clearSpecial() {
        if (specialBuilder_ == null) {
          special_ = java.util.Collections.emptyList();
          bitField0_ = (bitField0_ & ~0x00000004);
          onChanged();
        } else {
          specialBuilder_.clear();
        }
        return this;
      }
      /**
       * <code>repeated .Special special = 3;</code>
       */
      public Builder removeSpecial(int index) {
        if (specialBuilder_ == null) {
          ensureSpecialIsMutable();
          special_.remove(index);
          onChanged();
        } else {
          specialBuilder_.remove(index);
        }
        return this;
      }
      /**
       * <code>repeated .Special special = 3;</code>
       */
      public NightLiteProto.Special.Builder getSpecialBuilder(
          int index) {
        return internalGetSpecialFieldBuilder().getBuilder(index);
      }
      /**
       * <code>repeated .Special special = 3;</code>
       */
      public NightLiteProto.SpecialOrBuilder getSpecialOrBuilder(
          int index) {
        if (specialBuilder_ == null) {
          return special_.get(index);  } else {
          return specialBuilder_.getMessageOrBuilder(index);
        }
      }
      /**
       * <code>repeated .Special special = 3;</code>
       */
      public java.util.List<? extends NightLiteProto.SpecialOrBuilder> 
           getSpecialOrBuilderList() {
        if (specialBuilder_ != null) {
          return specialBuilder_.getMessageOrBuilderList();
        } else {
          return java.util.Collections.unmodifiableList(special_);
        }
      }
      /**
       * <code>repeated .Special special = 3;</code>
       */
      public NightLiteProto.Special.Builder addSpecialBuilder() {
        return internalGetSpecialFieldBuilder().addBuilder(
            NightLiteProto.Special.getDefaultInstance());
      }
      /**
       * <code>repeated .Special special = 3;</code>
       */
      public NightLiteProto.Special.Builder addSpecialBuilder(
          int index) {
        return internalGetSpecialFieldBuilder().addBuilder(
            index, NightLiteProto.Special.getDefaultInstance());
      }
      /**
       * <code>repeated .Special special = 3;</code>
       */
      public java.util.List<NightLiteProto.Special.Builder> 
           getSpecialBuilderList() {
        return internalGetSpecialFieldBuilder().getBuilderList();
      }
      private com.google.protobuf.RepeatedFieldBuilder<
          NightLiteProto.Special, NightLiteProto.Special.Builder, NightLiteProto.SpecialOrBuilder> 
          internalGetSpecialFieldBuilder() {
        if (specialBuilder_ == null) {
          specialBuilder_ = new com.google.protobuf.RepeatedFieldBuilder<
              NightLiteProto.Special, NightLiteProto.Special.Builder, NightLiteProto.SpecialOrBuilder>(
                  special_,
                  ((bitField0_ & 0x00000004) != 0),
                  getParentForChildren(),
                  isClean());
          special_ = null;
        }
        return specialBuilder_;
      }

      private java.util.List<NightLiteProto.KeyValuePair> meta_ =
        java.util.Collections.emptyList();
      private void ensureMetaIsMutable() {
        if (!((bitField0_ & 0x00000008) != 0)) {
          meta_ = new java.util.ArrayList<NightLiteProto.KeyValuePair>(meta_);
          bitField0_ |= 0x00000008;
         }
      }

      private com.google.protobuf.RepeatedFieldBuilder<
          NightLiteProto.KeyValuePair, NightLiteProto.KeyValuePair.Builder, NightLiteProto.KeyValuePairOrBuilder> metaBuilder_;

      /**
       * <code>repeated .KeyValuePair meta = 4;</code>
       */
      public java.util.List<NightLiteProto.KeyValuePair> getMetaList() {
        if (metaBuilder_ == null) {
          return java.util.Collections.unmodifiableList(meta_);
        } else {
          return metaBuilder_.getMessageList();
        }
      }
      /**
       * <code>repeated .KeyValuePair meta = 4;</code>
       */
      public int getMetaCount() {
        if (metaBuilder_ == null) {
          return meta_.size();
        } else {
          return metaBuilder_.getCount();
        }
      }
      /**
       * <code>repeated .KeyValuePair meta = 4;</code>
       */
      public NightLiteProto.KeyValuePair getMeta(int index) {
        if (metaBuilder_ == null) {
          return meta_.get(index);
        } else {
          return metaBuilder_.getMessage(index);
        }
      }
      /**
       * <code>repeated .KeyValuePair meta = 4;</code>
       */
      public Builder setMeta(
          int index, NightLiteProto.KeyValuePair value) {
        if (metaBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureMetaIsMutable();
          meta_.set(index, value);
          onChanged();
        } else {
          metaBuilder_.setMessage(index, value);
        }
        return this;
      }
      /**
       * <code>repeated .KeyValuePair meta = 4;</code>
       */
      public Builder setMeta(
          int index, NightLiteProto.KeyValuePair.Builder builderForValue) {
        if (metaBuilder_ == null) {
          ensureMetaIsMutable();
          meta_.set(index, builderForValue.build());
          onChanged();
        } else {
          metaBuilder_.setMessage(index, builderForValue.build());
        }
        return this;
      }
      /**
       * <code>repeated .KeyValuePair meta = 4;</code>
       */
      public Builder addMeta(NightLiteProto.KeyValuePair value) {
        if (metaBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureMetaIsMutable();
          meta_.add(value);
          onChanged();
        } else {
          metaBuilder_.addMessage(value);
        }
        return this;
      }
      /**
       * <code>repeated .KeyValuePair meta = 4;</code>
       */
      public Builder addMeta(
          int index, NightLiteProto.KeyValuePair value) {
        if (metaBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          ensureMetaIsMutable();
          meta_.add(index, value);
          onChanged();
        } else {
          metaBuilder_.addMessage(index, value);
        }
        return this;
      }
      /**
       * <code>repeated .KeyValuePair meta = 4;</code>
       */
      public Builder addMeta(
          NightLiteProto.KeyValuePair.Builder builderForValue) {
        if (metaBuilder_ == null) {
          ensureMetaIsMutable();
          meta_.add(builderForValue.build());
          onChanged();
        } else {
          metaBuilder_.addMessage(builderForValue.build());
        }
        return this;
      }
      /**
       * <code>repeated .KeyValuePair meta = 4;</code>
       */
      public Builder addMeta(
          int index, NightLiteProto.KeyValuePair.Builder builderForValue) {
        if (metaBuilder_ == null) {
          ensureMetaIsMutable();
          meta_.add(index, builderForValue.build());
          onChanged();
        } else {
          metaBuilder_.addMessage(index, builderForValue.build());
        }
        return this;
      }
      /**
       * <code>repeated .KeyValuePair meta = 4;</code>
       */
      public Builder addAllMeta(
          java.lang.Iterable<? extends NightLiteProto.KeyValuePair> values) {
        if (metaBuilder_ == null) {
          ensureMetaIsMutable();
          com.google.protobuf.AbstractMessageLite.Builder.addAll(
              values, meta_);
          onChanged();
        } else {
          metaBuilder_.addAllMessages(values);
        }
        return this;
      }
      /**
       * <code>repeated .KeyValuePair meta = 4;</code>
       */
      public Builder clearMeta() {
        if (metaBuilder_ == null) {
          meta_ = java.util.Collections.emptyList();
          bitField0_ = (bitField0_ & ~0x00000008);
          onChanged();
        } else {
          metaBuilder_.clear();
        }
        return this;
      }
      /**
       * <code>repeated .KeyValuePair meta = 4;</code>
       */
      public Builder removeMeta(int index) {
        if (metaBuilder_ == null) {
          ensureMetaIsMutable();
          meta_.remove(index);
          onChanged();
        } else {
          metaBuilder_.remove(index);
        }
        return this;
      }
      /**
       * <code>repeated .KeyValuePair meta = 4;</code>
       */
      public NightLiteProto.KeyValuePair.Builder getMetaBuilder(
          int index) {
        return internalGetMetaFieldBuilder().getBuilder(index);
      }
      /**
       * <code>repeated .KeyValuePair meta = 4;</code>
       */
      public NightLiteProto.KeyValuePairOrBuilder getMetaOrBuilder(
          int index) {
        if (metaBuilder_ == null) {
          return meta_.get(index);  } else {
          return metaBuilder_.getMessageOrBuilder(index);
        }
      }
      /**
       * <code>repeated .KeyValuePair meta = 4;</code>
       */
      public java.util.List<? extends NightLiteProto.KeyValuePairOrBuilder> 
           getMetaOrBuilderList() {
        if (metaBuilder_ != null) {
          return metaBuilder_.getMessageOrBuilderList();
        } else {
          return java.util.Collections.unmodifiableList(meta_);
        }
      }
      /**
       * <code>repeated .KeyValuePair meta = 4;</code>
       */
      public NightLiteProto.KeyValuePair.Builder addMetaBuilder() {
        return internalGetMetaFieldBuilder().addBuilder(
            NightLiteProto.KeyValuePair.getDefaultInstance());
      }
      /**
       * <code>repeated .KeyValuePair meta = 4;</code>
       */
      public NightLiteProto.KeyValuePair.Builder addMetaBuilder(
          int index) {
        return internalGetMetaFieldBuilder().addBuilder(
            index, NightLiteProto.KeyValuePair.getDefaultInstance());
      }
      /**
       * <code>repeated .KeyValuePair meta = 4;</code>
       */
      public java.util.List<NightLiteProto.KeyValuePair.Builder> 
           getMetaBuilderList() {
        return internalGetMetaFieldBuilder().getBuilderList();
      }
      private com.google.protobuf.RepeatedFieldBuilder<
          NightLiteProto.KeyValuePair, NightLiteProto.KeyValuePair.Builder, NightLiteProto.KeyValuePairOrBuilder> 
          internalGetMetaFieldBuilder() {
        if (metaBuilder_ == null) {
          metaBuilder_ = new com.google.protobuf.RepeatedFieldBuilder<
              NightLiteProto.KeyValuePair, NightLiteProto.KeyValuePair.Builder, NightLiteProto.KeyValuePairOrBuilder>(
                  meta_,
                  ((bitField0_ & 0x00000008) != 0),
                  getParentForChildren(),
                  isClean());
          meta_ = null;
        }
        return metaBuilder_;
      }

      private int units_ = 0;
      /**
       * <code>optional .Units units = 5;</code>
       * @return Whether the units field is set.
       */
      @java.lang.Override public boolean hasUnits() {
        return ((bitField0_ & 0x00000010) != 0);
      }
      /**
       * <code>optional .Units units = 5;</code>
       * @return The enum numeric value on the wire for units.
       */
      @java.lang.Override public int getUnitsValue() {
        return units_;
      }
      /**
       * <code>optional .Units units = 5;</code>
       * @param value The enum numeric value on the wire for units to set.
       * @throws IllegalArgumentException if UNRECOGNIZED is provided.
       * @return This builder for chaining.
       */
      public Builder setUnitsValue(int value) {
        units_ = value;
        bitField0_ |= 0x00000010;
        onChanged();
        return this;
      }
      /**
       * <code>optional .Units units = 5;</code>
       * @return The units.
       */
      @java.lang.Override
      public NightLiteProto.Units getUnits() {
        NightLiteProto.Units result = NightLiteProto.Units.forNumber(units_);
        return result == null ? NightLiteProto.Units.UNRECOGNIZED : result;
      }
      /**
       * <code>optional .Units units = 5;</code>
       * @param value The units to set.
       * @return This builder for chaining.
       */
      public Builder setUnits(NightLiteProto.Units value) {
        if (value == null) { throw new NullPointerException(); }
        bitField0_ |= 0x00000010;
        units_ = value.getNumber();
        onChanged();
        return this;
      }
      /**
       * <code>optional .Units units = 5;</code>
       * @return This builder for chaining.
       */
      public Builder clearUnits() {
        bitField0_ = (bitField0_ & ~0x00000010);
        units_ = 0;
        onChanged();
        return this;
      }

      // @@protoc_insertion_point(builder_scope:Block)
    }

    // @@protoc_insertion_point(class_scope:Block)
    private static final NightLiteProto.Block DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new NightLiteProto.Block();
    }

    public static NightLiteProto.Block getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<Block>
        PARSER = new com.google.protobuf.AbstractParser<Block>() {
      @java.lang.Override
      public Block parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        Builder builder = newBuilder();
        try {
          builder.mergeFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          throw e.setUnfinishedMessage(builder.buildPartial());
        } catch (com.google.protobuf.UninitializedMessageException e) {
          throw e.asInvalidProtocolBufferException().setUnfinishedMessage(builder.buildPartial());
        } catch (java.io.IOException e) {
          throw new com.google.protobuf.InvalidProtocolBufferException(e)
              .setUnfinishedMessage(builder.buildPartial());
        }
        return builder.buildPartial();
      }
    };

    public static com.google.protobuf.Parser<Block> parser() {
      return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<Block> getParserForType() {
      return PARSER;
    }

    @java.lang.Override
    public NightLiteProto.Block getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_GlucoseEntry_descriptor;
  private static final 
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_GlucoseEntry_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_Treatment_descriptor;
  private static final 
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_Treatment_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_KeyValuePair_descriptor;
  private static final 
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_KeyValuePair_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_Special_descriptor;
  private static final 
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_Special_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_Block_descriptor;
  private static final 
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_Block_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static final com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\'app/src/main/proto/NightLiteProto.prot" +
      "o\"X\n\014GlucoseEntry\022\017\n\007glucose\030\001 \001(\r\022\021\n\tti" +
      "mestamp\030\002 \001(\r\022\r\n\005delta\030\003 \001(\005\022\025\n\005trend\030\004 " +
      "\001(\0162\006.Trend\"<\n\tTreatment\022\021\n\ttimestamp\030\001 " +
      "\001(\r\022\r\n\005carbs\030\002 \001(\r\022\r\n\005units\030\003 \001(\r\"*\n\014Key" +
      "ValuePair\022\013\n\003key\030\001 \001(\t\022\r\n\005value\030\002 \001(\t\")\n" +
      "\007Special\022\021\n\ttimestamp\030\001 \001(\r\022\013\n\003msg\030\002 \001(\t" +
      "\"\245\001\n\005Block\022\036\n\007entries\030\001 \003(\0132\r.GlucoseEnt" +
      "ry\022\036\n\ntreatments\030\002 \003(\0132\n.Treatment\022\031\n\007sp" +
      "ecial\030\003 \003(\0132\010.Special\022\033\n\004meta\030\004 \003(\0132\r.Ke" +
      "yValuePair\022\032\n\005units\030\005 \001(\0162\006.UnitsH\000\210\001\001B\010" +
      "\n\006_units*\254\001\n\005Trend\022\021\n\rTREND_UNKNOWN\020\000\022\010\n" +
      "\004NONE\020\001\022\r\n\tDOUBLE_UP\020\002\022\r\n\tSINGLE_UP\020\003\022\t\n" +
      "\005UP_45\020\004\022\010\n\004FLAT\020\005\022\013\n\007DOWN_45\020\006\022\017\n\013SINGL" +
      "E_DOWN\020\007\022\017\n\013DOUBLE_DOWN\020\010\022\022\n\016NOT_COMPUTA" +
      "BLE\020\t\022\020\n\014OUT_OF_RANGE\020\n*\033\n\005Units\022\010\n\004MGDL" +
      "\020\000\022\010\n\004MMOL\020\001b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        });
    internal_static_GlucoseEntry_descriptor =
      getDescriptor().getMessageType(0);
    internal_static_GlucoseEntry_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessage.FieldAccessorTable(
        internal_static_GlucoseEntry_descriptor,
        new java.lang.String[] { "Glucose", "Timestamp", "Delta", "Trend", });
    internal_static_Treatment_descriptor =
      getDescriptor().getMessageType(1);
    internal_static_Treatment_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessage.FieldAccessorTable(
        internal_static_Treatment_descriptor,
        new java.lang.String[] { "Timestamp", "Carbs", "Units", });
    internal_static_KeyValuePair_descriptor =
      getDescriptor().getMessageType(2);
    internal_static_KeyValuePair_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessage.FieldAccessorTable(
        internal_static_KeyValuePair_descriptor,
        new java.lang.String[] { "Key", "Value", });
    internal_static_Special_descriptor =
      getDescriptor().getMessageType(3);
    internal_static_Special_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessage.FieldAccessorTable(
        internal_static_Special_descriptor,
        new java.lang.String[] { "Timestamp", "Msg", });
    internal_static_Block_descriptor =
      getDescriptor().getMessageType(4);
    internal_static_Block_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessage.FieldAccessorTable(
        internal_static_Block_descriptor,
        new java.lang.String[] { "Entries", "Treatments", "Special", "Meta", "Units", });
    descriptor.resolveAllFeaturesImmutable();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
