package com.sk89q.jnbt;

import java.util.ArrayList;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The {@code TAG_List} tag.
 */
public final class ListTag<T extends Tag> extends Tag {

    private final Class<T> type;
    private final List<T> value;

    /**
     * Creates the tag with an empty name.
     *
     * @param type  the type of tag
     * @param value the value of the tag
     */
    public ListTag(Class<T> type, List<T> value) {
        super();
        checkNotNull(value);
        this.type = type;
        this.value = value;
    }

    public static Class<?> inject() {
        return ListTag.class;
    }

    @Override
    public List<Object> getRaw() {
        ArrayList<Object> raw = new ArrayList<>();
        for (Tag t : value) {
            raw.add(t.getRaw());
        }
        return raw;
    }

    /**
     * Gets the type of item in this list.
     *
     * @return The type of item in this list.
     */
    public Class<T> getType() {
        return type;
    }

    @Override
    public List<T> getValue() {
        return value;
    }

    /**
     * Create a new list tag with this tag's name and type.
     *
     * @param list the new list
     * @return a new list tag
     */
    public ListTag setValue(List<Tag> list) {
        return new ListTag(getType(), list);
    }

    /**
     * Get the tag if it exists at the given index.
     *
     * @param index the index
     * @return the tag or null
     */
    @Nullable
    public Tag getIfExists(int index) {
        try {
            return value.get(index);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    /**
     * Get a byte array named with the given index.
     *
     * <p>If the index does not exist or its value is not a byte array tag,
     * then an empty byte array will be returned.</p>
     *
     * @param index the index
     * @return a byte array
     */
    public byte[] getByteArray(int index) {
        Tag tag = getIfExists(index);
        if (tag instanceof ByteArrayTag) {
            return ((ByteArrayTag) tag).getValue();
        } else {
            return new byte[0];
        }
    }

    /**
     * Get a byte named with the given index.
     *
     * <p>If the index does not exist or its value is not a byte tag,
     * then {@code 0} will be returned.</p>
     *
     * @param index the index
     * @return a byte
     */
    public byte getByte(int index) {
        Tag tag = getIfExists(index);
        if (tag instanceof ByteTag) {
            return ((ByteTag) tag).getValue();
        } else {
            return (byte) 0;
        }
    }

    /**
     * Get a double named with the given index.
     *
     * <p>If the index does not exist or its value is not a double tag,
     * then {@code 0} will be returned.</p>
     *
     * @param index the index
     * @return a double
     */
    public double getDouble(int index) {
        Tag tag = getIfExists(index);
        if (tag instanceof DoubleTag) {
            return ((DoubleTag) tag).getValue();
        } else {
            return 0;
        }
    }

    /**
     * Get a double named with the given index, even if it's another
     * type of number.
     *
     * <p>If the index does not exist or its value is not a number,
     * then {@code 0} will be returned.</p>
     *
     * @param index the index
     * @return a double
     */
    public double asDouble(int index) {
        Tag tag = getIfExists(index);
        if (tag instanceof ByteTag) {
            return ((ByteTag) tag).getValue();

        } else if (tag instanceof ShortTag) {
            return ((ShortTag) tag).getValue();

        } else if (tag instanceof IntTag) {
            return ((IntTag) tag).getValue();

        } else if (tag instanceof LongTag) {
            return ((LongTag) tag).getValue();

        } else if (tag instanceof FloatTag) {
            return ((FloatTag) tag).getValue();

        } else if (tag instanceof DoubleTag) {
            return ((DoubleTag) tag).getValue();

        } else {
            return 0;
        }
    }

    /**
     * Get a float named with the given index.
     *
     * <p>If the index does not exist or its value is not a float tag,
     * then {@code 0} will be returned.</p>
     *
     * @param index the index
     * @return a float
     */
    public float getFloat(int index) {
        Tag tag = getIfExists(index);
        if (tag instanceof FloatTag) {
            return ((FloatTag) tag).getValue();
        } else {
            return 0;
        }
    }

    /**
     * Get a {@code int[]} named with the given index.
     *
     * <p>If the index does not exist or its value is not an int array tag,
     * then an empty array will be returned.</p>
     *
     * @param index the index
     * @return an int array
     */
    public int[] getIntArray(int index) {
        Tag tag = getIfExists(index);
        if (tag instanceof IntArrayTag) {
            return ((IntArrayTag) tag).getValue();
        } else {
            return new int[0];
        }
    }

    /**
     * Get an int named with the given index.
     *
     * <p>If the index does not exist or its value is not an int tag,
     * then {@code 0} will be returned.</p>
     *
     * @param index the index
     * @return an int
     */
    public int getInt(int index) {
        Tag tag = getIfExists(index);
        if (tag instanceof IntTag) {
            return ((IntTag) tag).getValue();
        } else {
            return 0;
        }
    }

    /**
     * Get an int named with the given index, even if it's another
     * type of number.
     *
     * <p>If the index does not exist or its value is not a number,
     * then {@code 0} will be returned.</p>
     *
     * @param index the index
     * @return an int
     */
    public int asInt(int index) {
        Tag tag = getIfExists(index);
        if (tag instanceof ByteTag) {
            return ((ByteTag) tag).getValue();

        } else if (tag instanceof ShortTag) {
            return ((ShortTag) tag).getValue();

        } else if (tag instanceof IntTag) {
            return ((IntTag) tag).getValue();

        } else if (tag instanceof LongTag) {
            return ((LongTag) tag).getValue().intValue();

        } else if (tag instanceof FloatTag) {
            return ((FloatTag) tag).getValue().intValue();

        } else if (tag instanceof DoubleTag) {
            return ((DoubleTag) tag).getValue().intValue();

        } else {
            return 0;
        }
    }

    /**
     * Get a list of tags named with the given index.
     *
     * <p>If the index does not exist or its value is not a list tag,
     * then an empty list will be returned.</p>
     *
     * @param index the index
     * @return a list of tags
     */
    public List<Tag> getList(int index) {
        Tag tag = getIfExists(index);
        if (tag instanceof ListTag) {
            return ((ListTag) tag).getValue();
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Get a {@code TagList} named with the given index.
     *
     * <p>If the index does not exist or its value is not a list tag,
     * then an empty tag list will be returned.</p>
     *
     * @param index the index
     * @return a tag list instance
     */
    public ListTag getListTag(int index) {
        Tag tag = getIfExists(index);
        if (tag instanceof ListTag) {
            return (ListTag) tag;
        } else {
            return new ListTag(StringTag.class, Collections.<Tag>emptyList());
        }
    }

    /**
     * Get a list of tags named with the given index.
     *
     * <p>If the index does not exist or its value is not a list tag,
     * then an empty list will be returned. If the given index references
     * a list but the list of of a different type, then an empty
     * list will also be returned.</p>
     *
     * @param index    the index
     * @param listType the class of the contained type
     * @param <T>      the NBT type
     * @return a list of tags
     */
    @SuppressWarnings("unchecked")
    public <T extends Tag> List<T> getList(int index, Class<T> listType) {
        Tag tag = getIfExists(index);
        if (tag instanceof ListTag) {
            ListTag listTag = (ListTag) tag;
            if (listTag.getType().equals(listType)) {
                return (List<T>) listTag.getValue();
            } else {
                return Collections.emptyList();
            }
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Get a long named with the given index.
     *
     * <p>If the index does not exist or its value is not a long tag,
     * then {@code 0} will be returned.</p>
     *
     * @param index the index
     * @return a long
     */
    public long getLong(int index) {
        Tag tag = getIfExists(index);
        if (tag instanceof LongTag) {
            return ((LongTag) tag).getValue();
        } else {
            return 0L;
        }
    }

    /**
     * Get a long named with the given index, even if it's another
     * type of number.
     *
     * <p>If the index does not exist or its value is not a number,
     * then {@code 0} will be returned.</p>
     *
     * @param index the index
     * @return a long
     */
    public long asLong(int index) {
        Tag tag = getIfExists(index);
        if (tag instanceof ByteTag) {
            return ((ByteTag) tag).getValue();

        } else if (tag instanceof ShortTag) {
            return ((ShortTag) tag).getValue();

        } else if (tag instanceof IntTag) {
            return ((IntTag) tag).getValue();

        } else if (tag instanceof LongTag) {
            return ((LongTag) tag).getValue();

        } else if (tag instanceof FloatTag) {
            return ((FloatTag) tag).getValue().longValue();

        } else if (tag instanceof DoubleTag) {
            return ((DoubleTag) tag).getValue().longValue();

        } else {
            return 0;
        }
    }

    /**
     * Get a short named with the given index.
     *
     * <p>If the index does not exist or its value is not a short tag,
     * then {@code 0} will be returned.</p>
     *
     * @param index the index
     * @return a short
     */
    public short getShort(int index) {
        Tag tag = getIfExists(index);
        if (tag instanceof ShortTag) {
            return ((ShortTag) tag).getValue();
        } else {
            return 0;
        }
    }

    /**
     * Get a string named with the given index.
     *
     * <p>If the index does not exist or its value is not a string tag,
     * then {@code ""} will be returned.</p>
     *
     * @param index the index
     * @return a string
     */
    public String getString(int index) {
        Tag tag = getIfExists(index);
        if (tag instanceof StringTag) {
            return ((StringTag) tag).getValue();
        } else {
            return "";
        }
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("TAG_List").append(": ").append(value.size()).append(" entries of type ").append(NBTUtils.getTypeName(type)).append("\r\n{\r\n");
        for (Tag t : value) {
            bldr.append("   ").append(t.toString().replaceAll("\r\n", "\r\n   ")).append("\r\n");
        }
        bldr.append("}");
        return bldr.toString();
    }

}