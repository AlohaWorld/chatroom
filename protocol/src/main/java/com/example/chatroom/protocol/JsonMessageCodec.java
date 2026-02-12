package com.example.chatroom.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

import java.util.List;

public class JsonMessageCodec extends MessageToMessageCodec<ByteBuf, Message> {
    private final ObjectMapper mapper;

    public JsonMessageCodec() {
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, List<Object> out) throws Exception {
        try {
            byte[] bytes = mapper.writeValueAsBytes(msg);
            ByteBuf buf = Unpooled.wrappedBuffer(bytes);
            out.add(buf);
        } catch (JsonProcessingException ex) {
            throw new MessageDecodeException("encode_failed", ex);
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        byte[] bytes = new byte[in.readableBytes()];
        in.readBytes(bytes);
        try {
            Message msg = mapper.readValue(bytes, Message.class);
            out.add(msg);
        } catch (Exception ex) {
            throw new MessageDecodeException("decode_failed", ex);
        }
    }
}
