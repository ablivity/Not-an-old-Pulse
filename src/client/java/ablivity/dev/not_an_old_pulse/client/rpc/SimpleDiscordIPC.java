package ablivity.dev.not_an_old_pulse.client.rpc;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

public class SimpleDiscordIPC {
    private static RandomAccessFile pipe;
    private static boolean connected = false;

    public static void start() {
        new Thread(() -> {
            try {
                pipe = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-0", "rw");
                String handshake = "{\"v\":1,\"client_id\":\"1121853612479262791\"}";
                send(0, handshake);
                connected = true;
                
                while (connected) {
                    byte[] header = new byte[8];
                    if (pipe.read(header) == -1) break;
                }
            } catch (Exception e) {
                connected = false;
            }
        }).start();
    }

    public static void update(String details, String state) {
        if (!connected || pipe == null) return;
        try {
            String json = "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":" + ProcessHandle.current().pid() + ",\"activity\":{\"state\":\"" + state + "\",\"details\":\"" + details + "\",\"assets\":{\"large_image\":\"minecraft\",\"large_text\":\"Not an old Pulse\"},\"timestamps\":{\"start\":" + (System.currentTimeMillis() / 1000) + "}}},\"nonce\":\"" + UUID.randomUUID() + "\"}";
            send(1, json);
        } catch (Exception e) {
        }
    }

    private static void send(int op, String json) throws Exception {
        byte[] d = json.getBytes("UTF-8");
        ByteBuffer buf = ByteBuffer.allocate(8 + d.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(op);
        buf.putInt(d.length);
        buf.put(d);
        pipe.write(buf.array());
    }
}

