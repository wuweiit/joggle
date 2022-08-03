package com.wuweibi.bullet.protocol;
/**
 * Created by marker on 2017/12/7.
 */

import com.wuweibi.bullet.utils.Utils;
import lombok.Data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 任意门配置
 *
 * @author marker
 * @create 2017-12-07 下午1:13
 **/

@Data
public class MsgDeviceDoor extends Message {

    /**
     *  json 配置
     */
    private String content;


    /**
     * 构造
     */
    public MsgDeviceDoor(String content) {
        super(Message.DEVICE_DOOR);
        this.content = content;
        int len = this.content.length();
        getHead().setLength(super.getLength() + len);
    }

    public MsgDeviceDoor() {
        super(Message.DEVICE_DOOR);
        int len = this.content.length();
        getHead().setLength(super.getLength() + len);
    }


    @Override
    public void write(OutputStream out) throws IOException {
        getHead().write(out);
        byte bs[] = this.content.getBytes(StandardCharsets.UTF_8) ;
        out.write(bs);
        out.flush();
    }

    @Override
    public void read(InputStream in) throws IOException {
        int len = super.getLength();
        len = len - MsgHead.HEAD_LENGTH;
        // 读取deviceNo
        byte bs[] = new byte[len];
        in.read(bs);
        this.content = Utils.getString(bs, 0, len);
    }


}
