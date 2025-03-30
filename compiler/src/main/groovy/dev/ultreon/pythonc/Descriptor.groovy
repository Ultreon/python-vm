package dev.ultreon.pythonc

import com.google.common.collect.ImmutableList
import org.objectweb.asm.Type

class Descriptor {
    static ImmutableList<Type> parse(String desc) {
        StringReader stringReader = new StringReader(desc)
        ImmutableList.Builder<Type> builder = ImmutableList.builder()
        try {
            while (true) {
                Type element = readOne(stringReader)
                if (element == null) break
                builder.add(element)
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e)
        }

        return builder.build()
    }

    private static Type readOne(StringReader stringReader) throws IOException {
        int read = stringReader.read()
        if (read == -1) {
            return null
        }

        StringBuilder sb = new StringBuilder()
        if (read == ('[' as char)) {
            sb.append(readOne(stringReader))
            return Type.getType(sb.toString())
        }

        if (read == ('L' as char)) {
            while (true) {
                read = stringReader.read()
                if (read == -1) {
                    throw new IllegalArgumentException("unclosed string")
                }
                if (read == (';' as char)) {
                    break
                }
                sb.append((char) read)
            }
            return Type.getType("L" + sb + ";")
        }
        sb.append((char) read)
        return Type.getType(sb.toString())
    }
}
