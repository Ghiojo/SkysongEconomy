package org.skysongdev.skysongEconomy.items;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Item stacks kept exactly as they were, such as a planted primer, so the same items can be
 * given back later. Each stack is the server's own serialized form; turning stacks into these
 * bytes and back is left to the server glue, so this class has no server classes.
 */
public final class StoredItems {
    private final List<byte[]> stacks;

    private StoredItems(List<byte[]> stacks) {
        List<byte[]> copies = new ArrayList<>(stacks.size());
        for (byte[] stack : stacks) {
            if (stack.length == 0) {
                throw new IllegalArgumentException("A stored stack can't be empty");
            }
            copies.add(stack.clone());
        }
        if (copies.isEmpty()) {
            throw new IllegalArgumentException("Stored items need at least one stack");
        }
        this.stacks = List.copyOf(copies);
    }

    /** @param stacks each stack's serialized bytes; at least one */
    public static StoredItems of(List<byte[]> stacks) {
        return new StoredItems(stacks);
    }

    /** @return each stack's serialized bytes, as copies */
    public List<byte[]> stacks() {
        return stacks.stream().map(byte[]::clone).toList();
    }

    /** @return every stack in one array, for one database column */
    public byte[] toBytes() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(stacks.size());
            for (byte[] stack : stacks) {
                out.writeInt(stack.length);
                out.write(stack);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    /**
     * @param bytes what {@link #toBytes()} wrote
     * @throws IllegalArgumentException when the bytes are cut short or not in that form
     */
    public static StoredItems fromBytes(byte[] bytes) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            int count = in.readInt();
            if (count < 1 || count > bytes.length) {
                throw new IllegalArgumentException("Stored items name " + count + " stacks");
            }
            List<byte[]> stacks = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                int length = in.readInt();
                if (length < 1 || length > in.available()) {
                    throw new IllegalArgumentException("Stored stack " + i + " is cut short");
                }
                stacks.add(in.readNBytes(length));
            }
            if (in.available() > 0) {
                throw new IllegalArgumentException("Stored items have bytes left over");
            }
            return new StoredItems(stacks);
        } catch (IOException e) {
            throw new IllegalArgumentException("Stored items are cut short", e);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof StoredItems that) || that.stacks.size() != stacks.size()) {
            return false;
        }
        for (int i = 0; i < stacks.size(); i++) {
            if (!Arrays.equals(stacks.get(i), that.stacks.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 1;
        for (byte[] stack : stacks) {
            hash = 31 * hash + Arrays.hashCode(stack);
        }
        return hash;
    }

    @Override
    public String toString() {
        return "StoredItems[" + stacks.size() + " stack(s)]";
    }
}
