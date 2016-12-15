package mpo.dayon.common.network.message;

import mpo.dayon.assisted.compressor.CompressorEngineConfiguration;
import mpo.dayon.common.buffer.MemByteBuffer;
import mpo.dayon.common.squeeze.CompressionMethod;
import mpo.dayon.common.utils.UnitUtilities;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class NetworkCaptureMessage extends NetworkMessage
{
    private final int id;

    private final CompressionMethod compressionMethod;

    @Nullable
    private final CompressorEngineConfiguration compressionConfiguration;

    private final MemByteBuffer payload;


    public NetworkCaptureMessage(int id,
                                 CompressionMethod compressionMethod,
                                 @Nullable CompressorEngineConfiguration compressionConfiguration,
                                 MemByteBuffer payload)
    {
        this.id = id;
        this.compressionMethod = compressionMethod;
        this.compressionConfiguration = compressionConfiguration;
        this.payload = payload;
    }

    public NetworkMessageType getType()
    {
        return NetworkMessageType.CAPTURE;
    }

    public int getId()
    {
        return id;
    }

    public CompressionMethod getCompressionMethod()
    {
        return compressionMethod;
    }

    @Nullable
    public CompressorEngineConfiguration getCompressionConfiguration()
    {
        return compressionConfiguration;
    }

    /**
     * Take into account some extra-info sent over the network with the actual payload ...
     */
    public int getWireSize()
    {
        if (compressionConfiguration == null)
        {
            return 11 + payload.size(); // type (byte) + capture-id (int) + compression (byte) + configuration-marker (byte) + len (int) + data (byte[])
        }
        else
        {
            return 10 + 11 + payload.size(); // type (byte) + capture-id (int) + compression (byte) + configuration (???) + len (int) + data (byte[])
        }
    }

    public void marshall(DataOutputStream out) throws IOException
    {
        marshallEnum(out, NetworkMessageType.class, getType());

        // debugging info - might need it before decompressing the payload (!)
        out.writeInt(id);

        // allows for decompressing on the other side ...
        marshallEnum(out, CompressionMethod.class, compressionMethod);

        out.writeByte(compressionConfiguration != null ? 1 : 0);

        if (compressionConfiguration != null)
        {
            new NetworkCompressorConfigurationMessage(compressionConfiguration).marshall(out);
        }

        out.writeInt(payload.size());
        out.write(payload.getInternal(), 0, payload.size());
    }

    public static NetworkCaptureMessage unmarshall(DataInputStream in) throws IOException
    {
        final int id = in.readInt();
        final CompressionMethod compressionMethod = unmarshallEnum(in, CompressionMethod.class);

        @Nullable
        final CompressorEngineConfiguration compressionConfiguration;

        if (in.readByte() == 1)
        {
            NetworkMessage.unmarshallEnum(in, NetworkMessageType.class);
            compressionConfiguration = NetworkCompressorConfigurationMessage.unmarshall(in).getConfiguration();
        }
        else
        {
            compressionConfiguration = null;
        }

        final int len = in.readInt();
        final byte[] data = new byte[len];

        int offset = 0;
        int count;

        while ((count = in.read(data, offset, data.length - offset)) > 0)
        {
            offset += count;
        }

        return new NetworkCaptureMessage(id, compressionMethod, compressionConfiguration, new MemByteBuffer(data));
    }

    public MemByteBuffer getPayload()
    {
        return payload;
    }

    public String toString()
    {
        return String.format("[id:%d] [%s]", id, UnitUtilities.toBitSize(8 * payload.size()));
    }
}