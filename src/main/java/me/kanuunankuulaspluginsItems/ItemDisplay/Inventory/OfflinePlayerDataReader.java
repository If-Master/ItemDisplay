package me.kanuunankuulaspluginsItems.ItemDisplay.Inventory;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


public class OfflinePlayerDataReader {

    private static boolean DEBUG = false; // Set to false in production

    public static ViewInvAdmin.OfflineInventoryData readPlayerInventory(File playerFile) throws Exception {
        ViewInvAdmin.OfflineInventoryData data = new ViewInvAdmin.OfflineInventoryData();
        data.playerFile = playerFile;

        if (DEBUG) System.out.println("[NBT DEBUG] Reading player file: " + playerFile.getName());

        Object nbtTagCompound = readNBTFromFile(playerFile);
        data.nbtCompound = nbtTagCompound;

        if (DEBUG) System.out.println("[NBT DEBUG] NBT compound class: " + (nbtTagCompound != null ? nbtTagCompound.getClass().getName() : "null"));

        Object inventoryList = getNBTList(nbtTagCompound, "Inventory");
        if (DEBUG) System.out.println("[NBT DEBUG] Inventory list: " + (inventoryList != null ? inventoryList.getClass().getName() : "null"));

        if (inventoryList != null) {
            int listSize = getNBTListSize(inventoryList);
            if (DEBUG) System.out.println("[NBT DEBUG] Inventory list size: " + listSize);

            for (int i = 0; i < listSize; i++) {
                Object itemTag = getNBTListElement(inventoryList, i);
                byte slot = getNBTByte(itemTag, "Slot");

                if (slot >= 0 && slot < 41) {
                    ItemStack item = itemStackFromNBT(itemTag);
                    if (DEBUG && item != null) System.out.println("[NBT DEBUG] Found item in slot " + slot + ": " + item.getType());

                    if (slot >= 0 && slot <= 8) {
                        data.inventory[slot] = item;
                    } else if (slot >= 9 && slot <= 35) {
                        data.inventory[slot] = item;
                    } else if (slot >= 100 && slot <= 103) {
                        data.armor[slot - 100] = item;
                    } else if (slot == -106) {
                        data.offhand = item;
                    }
                }
            }
        } else {
            if (DEBUG) System.out.println("[NBT DEBUG] WARNING: Inventory list is null!");
        }

        Object enderList = getNBTList(nbtTagCompound, "EnderItems");
        if (DEBUG) System.out.println("[NBT DEBUG] EnderItems list: " + (enderList != null ? enderList.getClass().getName() : "null"));

        if (enderList != null) {
            int listSize = getNBTListSize(enderList);
            if (DEBUG) System.out.println("[NBT DEBUG] EnderItems list size: " + listSize);

            for (int i = 0; i < listSize; i++) {
                Object itemTag = getNBTListElement(enderList, i);
                byte slot = getNBTByte(itemTag, "Slot");

                if (slot >= 0 && slot < 27) {
                    data.enderChest[slot] = itemStackFromNBT(itemTag);
                }
            }
        } else {
            if (DEBUG) System.out.println("[NBT DEBUG] WARNING: EnderItems list is null!");
        }

        return data;
    }

    public static void writePlayerInventory(File playerFile, ViewInvAdmin.OfflineInventoryData data) throws Exception {
        try {
            Object nbtTagCompound = data.nbtCompound;

            if (nbtTagCompound == null) {
                throw new Exception("NBT compound is null - cannot save data!");
            }

            Object inventoryList = createNBTList();

            for (int i = 0; i < 9; i++) {
                if (data.inventory[i] != null) {
                    Object itemTag = itemStackToNBT(data.inventory[i]);
                    setNBTByte(itemTag, "Slot", (byte) i);
                    addToNBTList(inventoryList, itemTag);
                }
            }

            for (int i = 9; i < 36; i++) {
                if (data.inventory[i] != null) {
                    Object itemTag = itemStackToNBT(data.inventory[i]);
                    setNBTByte(itemTag, "Slot", (byte) i);
                    addToNBTList(inventoryList, itemTag);
                }
            }

            for (int i = 0; i < 4; i++) {
                if (data.armor[i] != null) {
                    Object itemTag = itemStackToNBT(data.armor[i]);
                    setNBTByte(itemTag, "Slot", (byte) (100 + i));
                    addToNBTList(inventoryList, itemTag);
                }
            }

            if (data.offhand != null) {
                Object itemTag = itemStackToNBT(data.offhand);
                setNBTByte(itemTag, "Slot", (byte) -106);
                addToNBTList(inventoryList, itemTag);
            }

            setNBTList(nbtTagCompound, "Inventory", inventoryList);

            Object enderList = createNBTList();
            for (int i = 0; i < 27; i++) {
                if (data.enderChest[i] != null) {
                    Object itemTag = itemStackToNBT(data.enderChest[i]);
                    setNBTByte(itemTag, "Slot", (byte) i);
                    addToNBTList(enderList, itemTag);
                }
            }

            setNBTList(nbtTagCompound, "EnderItems", enderList);

            writeNBTToFile(playerFile, nbtTagCompound);

            if (DEBUG) {
                System.out.println("[NBT DEBUG] Successfully wrote player data to file");
            }

        } catch (Exception e) {
            if (DEBUG) {
                System.out.println("[NBT DEBUG] ERROR during write: " + e.getMessage());
                e.printStackTrace();
            }
            throw new Exception("Failed to write player data: " + e.getMessage(), e);
        }
    }

    private static Object readNBTFromFile(File file) throws Exception {
        try {
            Class<?> nbtIo = Class.forName("net.minecraft.nbt.NbtIo");
            Class<?> nbtAccounter = Class.forName("net.minecraft.nbt.NbtAccounter");

            Object unlimitedAccounter = null;
            try {
                Field unlimitedHeap = nbtAccounter.getField("unlimitedHeap");
                unlimitedAccounter = unlimitedHeap.get(null);
            } catch (NoSuchFieldException e) {
                try {
                    Field unlimited = nbtAccounter.getField("UNLIMITED");
                    unlimitedAccounter = unlimited.get(null);
                } catch (NoSuchFieldException e2) {
                    try {
                        Field a = nbtAccounter.getField("a");
                        unlimitedAccounter = a.get(null);
                    } catch (NoSuchFieldException e3) {
                        Method create = nbtAccounter.getMethod("create", long.class);
                        unlimitedAccounter = create.invoke(null, Long.MAX_VALUE);
                    }
                }
            }

            Method readMethod = null;
            Object result = null;

            try {
                readMethod = nbtIo.getMethod("readCompressed", java.nio.file.Path.class, nbtAccounter);
                result = readMethod.invoke(null, file.toPath(), unlimitedAccounter);
                return result;
            } catch (NoSuchMethodException e1) {
                try {
                    readMethod = nbtIo.getMethod("readCompressed", InputStream.class, nbtAccounter);
                    try (InputStream is = new FileInputStream(file)) {
                        result = readMethod.invoke(null, is, unlimitedAccounter);
                        return result;
                    }
                } catch (NoSuchMethodException e2) {
                    try {
                        readMethod = nbtIo.getMethod("a", java.nio.file.Path.class, nbtAccounter);
                        result = readMethod.invoke(null, file.toPath(), unlimitedAccounter);
                        return result;
                    } catch (NoSuchMethodException e3) {
                        throw new Exception("Could not find suitable readCompressed method in NbtIo");
                    }
                }
            }

        } catch (ClassNotFoundException e) {
        }

        try (DataInputStream dis = new DataInputStream(new GZIPInputStream(new FileInputStream(file)))) {
            try {
                Class<?> nbtCompressedStreamTools = Class.forName("net.minecraft.nbt.NBTCompressedStreamTools");
                Method readMethod = nbtCompressedStreamTools.getMethod("a", DataInputStream.class);
                return readMethod.invoke(null, dis);
            } catch (ClassNotFoundException | NoSuchMethodException e2) {
                String version = getServerVersion();
                Class<?> nbtCompressedStreamTools = Class.forName("net.minecraft.server." + version + ".NBTCompressedStreamTools");
                Method readMethod = nbtCompressedStreamTools.getMethod("a", DataInputStream.class);
                return readMethod.invoke(null, dis);
            }
        }
    }

    private static void writeNBTToFile(File file, Object nbtTag) throws Exception {
        try {
            Class<?> nbtIo = Class.forName("net.minecraft.nbt.NbtIo");
            Class<?> nbtCompound = nbtTag.getClass();
            Class<?> nbtAccounter = Class.forName("net.minecraft.nbt.NbtAccounter");

            Object unlimitedAccounter = null;
            try {
                Field unlimitedHeap = nbtAccounter.getField("unlimitedHeap");
                unlimitedAccounter = unlimitedHeap.get(null);
            } catch (NoSuchFieldException e) {
                try {
                    Field unlimited = nbtAccounter.getField("UNLIMITED");
                    unlimitedAccounter = unlimited.get(null);
                } catch (NoSuchFieldException e2) {
                    try {
                        Field a = nbtAccounter.getField("a");
                        unlimitedAccounter = a.get(null);
                    } catch (NoSuchFieldException e3) {
                        Method create = nbtAccounter.getMethod("create", long.class);
                        unlimitedAccounter = create.invoke(null, Long.MAX_VALUE);
                    }
                }
            }

            Method writeMethod = null;

            try {
                writeMethod = nbtIo.getMethod("writeCompressed", nbtCompound, java.nio.file.Path.class);
                writeMethod.invoke(null, nbtTag, file.toPath());
                return;
            } catch (NoSuchMethodException e1) {
                try {
                    writeMethod = nbtIo.getMethod("writeCompressed", nbtCompound, OutputStream.class);
                    try (OutputStream os = new FileOutputStream(file)) {
                        writeMethod.invoke(null, nbtTag, os);
                        return;
                    }
                } catch (NoSuchMethodException e2) {
                    try {
                        writeMethod = nbtIo.getMethod("a", nbtCompound, java.nio.file.Path.class);
                        writeMethod.invoke(null, nbtTag, file.toPath());
                        return;
                    } catch (NoSuchMethodException e3) {
                        throw new Exception("Could not find suitable writeCompressed method in NbtIo");
                    }
                }
            }

        } catch (ClassNotFoundException e) {
        }

        try (DataOutputStream dos = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(file)))) {
            try {
                Class<?> nbtCompressedStreamTools = Class.forName("net.minecraft.nbt.NBTCompressedStreamTools");
                Class<?> nbtTagCompoundClass = Class.forName("net.minecraft.nbt.NBTTagCompound");
                Method writeMethod = nbtCompressedStreamTools.getMethod("a", nbtTagCompoundClass, DataOutputStream.class);
                writeMethod.invoke(null, nbtTag, dos);
            } catch (ClassNotFoundException | NoSuchMethodException e2) {
                String version = getServerVersion();
                Class<?> nbtCompressedStreamTools = Class.forName("net.minecraft.server." + version + ".NBTCompressedStreamTools");
                Class<?> nbtTagCompoundClass = Class.forName("net.minecraft.server." + version + ".NBTTagCompound");
                Method writeMethod = nbtCompressedStreamTools.getMethod("a", nbtTagCompoundClass, DataOutputStream.class);
                writeMethod.invoke(null, nbtTag, dos);
            }
        }
    }

    private static ItemStack itemStackFromNBT(Object nbtTag) throws Exception {
        try {
            String id = getNBTString(nbtTag, "id");

            if (DEBUG) {
                System.out.println("[NBT DEBUG] ===== READING ITEM NBT =====");
                System.out.println("[NBT DEBUG] Item ID: " + id);

                try {
                    Method getAllKeys = nbtTag.getClass().getMethod("getAllKeys");
                    Object keys = getAllKeys.invoke(nbtTag);
                    System.out.println("[NBT DEBUG] All keys in item NBT: " + keys);
                } catch (Exception e) {
                    System.out.println("[NBT DEBUG] Could not get keys: " + e.getMessage());
                }

                try {
                    Method get = nbtTag.getClass().getMethod("get", String.class);
                    Object countTag = get.invoke(nbtTag, "count");
                    System.out.println("[NBT DEBUG] 'count' field: " + (countTag != null ? countTag.getClass().getName() + " = " + countTag : "null"));
                } catch (Exception e) {
                    System.out.println("[NBT DEBUG] 'count' field access failed: " + e.getMessage());
                }

                try {
                    Method get = nbtTag.getClass().getMethod("get", String.class);
                    Object countTag = get.invoke(nbtTag, "Count");
                    System.out.println("[NBT DEBUG] 'Count' field: " + (countTag != null ? countTag.getClass().getName() + " = " + countTag : "null"));
                } catch (Exception e) {
                    System.out.println("[NBT DEBUG] 'Count' field access failed: " + e.getMessage());
                }
            }

            int count = 1;

            try {
                Method getInt = nbtTag.getClass().getMethod("getInt", String.class);
                Object result = getInt.invoke(nbtTag, "count");

                if (DEBUG) System.out.println("[NBT DEBUG] getInt('count') returned: " + result + " (class: " + (result != null ? result.getClass().getName() : "null") + ")");

                if (result != null && result.getClass().getName().contains("Optional")) {
                    Method isPresent = result.getClass().getMethod("isPresent");
                    if ((Boolean) isPresent.invoke(result)) {
                        Method get = result.getClass().getMethod("get");
                        count = ((Number) get.invoke(result)).intValue();
                        if (DEBUG) System.out.println("[NBT DEBUG] Got count from Optional: " + count);
                    }
                } else if (result instanceof Number) {
                    count = ((Number) result).intValue();
                    if (DEBUG) System.out.println("[NBT DEBUG] Got count directly: " + count);
                }
            } catch (Exception e) {
                if (DEBUG) System.out.println("[NBT DEBUG] Failed to get 'count': " + e.getMessage());

                try {
                    byte countByte = getNBTByte(nbtTag, "Count");
                    count = countByte > 0 ? countByte : 1;
                    if (DEBUG) System.out.println("[NBT DEBUG] Got Count as byte: " + count);
                } catch (Exception e2) {
                    if (DEBUG) System.out.println("[NBT DEBUG] Failed to get 'Count': " + e2.getMessage());
                    count = 1;
                }
            }

            if (count <= 0) count = 1;

            if (DEBUG) {
                System.out.println("[NBT DEBUG] Final count: " + count);
                System.out.println("[NBT DEBUG] ==============================");
            }

            short damage = getNBTShort(nbtTag, "Damage");

            org.bukkit.Material material = getMaterialFromId(id);
            if (material == null || material == org.bukkit.Material.AIR) {
                return null;
            }

            ItemStack item = new ItemStack(material, count);

            if (damage > 0 && item.getType().getMaxDurability() > 0) {
                item.setDurability(damage);
            }

            Object tag = getNBTCompound(nbtTag, "tag");
            if (tag != null) {
                applyNBTToItem(item, tag);
            }

            return item;

        } catch (Exception e) {
            if (DEBUG) {
                System.out.println("[NBT DEBUG] Error creating ItemStack: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        }
    }
    private static Object itemStackToNBT(ItemStack item) throws Exception {
        Object nbtTag = createNBTCompound();

        String materialId = getMaterialId(item.getType());
        setNBTString(nbtTag, "id", materialId);

        int amount = item.getAmount();

        boolean modernFormatSet = false;
        try {
            Method putInt = nbtTag.getClass().getMethod("putInt", String.class, int.class);
            putInt.invoke(nbtTag, "count", amount);
            modernFormatSet = true;
            if (DEBUG) System.out.println("[NBT DEBUG] Saved count using modern format (lowercase 'count' as int): " + amount);
        } catch (NoSuchMethodException e) {
            try {
                Method setInt = nbtTag.getClass().getMethod("setInt", String.class, int.class);
                setInt.invoke(nbtTag, "count", amount);
                modernFormatSet = true;
                if (DEBUG) System.out.println("[NBT DEBUG] Saved count using modern format with setInt: " + amount);
            } catch (NoSuchMethodException e2) {
                if (DEBUG) System.out.println("[NBT DEBUG] Modern format (lowercase 'count') not available");
            }
        }

        try {
            setNBTByte(nbtTag, "Count", (byte) amount);
            if (DEBUG) System.out.println("[NBT DEBUG] Saved count using legacy format (uppercase 'Count' as byte): " + amount);
        } catch (Exception e) {
            if (DEBUG) System.out.println("[NBT DEBUG] Could not save legacy Count format: " + e.getMessage());
            if (!modernFormatSet) {
                throw new Exception("Failed to save item count in any format!");
            }
        }

        if (item.getDurability() > 0) {
            setNBTShort(nbtTag, "Damage", item.getDurability());
        }

        if (item.hasItemMeta()) {
            Object tagCompound = createNBTCompound();
            applyItemMetaToNBT(item, tagCompound);
            setNBTCompound(nbtTag, "tag", tagCompound);
        }

        return nbtTag;
    }

    private static void applyNBTToItem(ItemStack item, Object nbtTag) throws Exception {
        Object display = getNBTCompound(nbtTag, "display");
        if (display != null) {
            String name = getNBTString(display, "Name");
            if (name != null && !name.isEmpty()) {
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(name);
                item.setItemMeta(meta);
            }

            Object loreList = getNBTList(display, "Lore");
            if (loreList != null) {
                java.util.List<String> lore = new java.util.ArrayList<>();
                int size = getNBTListSize(loreList);
                for (int i = 0; i < size; i++) {
                    Object loreTag = getNBTListElement(loreList, i);
                    String loreLine = getNBTString(loreTag, "");
                    if (loreLine != null) {
                        lore.add(loreLine);
                    }
                }
                if (!lore.isEmpty()) {
                    org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
            }
        }
    }

    private static void applyItemMetaToNBT(ItemStack item, Object nbtTag) throws Exception {
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

        Object display = createNBTCompound();

        if (meta.hasDisplayName()) {
            setNBTString(display, "Name", meta.getDisplayName());
        }

        if (meta.hasLore()) {
            Object loreList = createNBTList();
            for (String line : meta.getLore()) {
                Object loreTag = createNBTString(line);
                addToNBTList(loreList, loreTag);
            }
            setNBTList(display, "Lore", loreList);
        }

        setNBTCompound(nbtTag, "display", display);
    }

    private static org.bukkit.Material getMaterialFromId(String id) {
        if (id == null) return null;
        id = id.replace("minecraft:", "").toUpperCase();
        try {
            return org.bukkit.Material.valueOf(id);
        } catch (IllegalArgumentException e) {
            return getLegacyMaterial(id);
        }
    }

    private static org.bukkit.Material getLegacyMaterial(String id) {
        switch (id.toUpperCase()) {
            case "GRASS": return org.bukkit.Material.valueOf("GRASS_BLOCK");
            case "WOOD": return org.bukkit.Material.valueOf("OAK_WOOD");
            case "LOG": return org.bukkit.Material.valueOf("OAK_LOG");
            case "STONE": return org.bukkit.Material.STONE;
            default:
                try {
                    return org.bukkit.Material.matchMaterial(id);
                } catch (Exception e) {
                    return org.bukkit.Material.STONE;
                }
        }
    }

    private static String getMaterialId(org.bukkit.Material material) {
        return "minecraft:" + material.name().toLowerCase();
    }

    private static Object createNBTCompound() throws Exception {
        try {
            Class<?> nbtCompound = Class.forName("net.minecraft.nbt.NbtCompound");
            return nbtCompound.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            try {
                Class<?> nbtCompound = Class.forName("net.minecraft.nbt.NBTTagCompound");
                return nbtCompound.getDeclaredConstructor().newInstance();
            } catch (ClassNotFoundException e2) {
                String version = getServerVersion();
                Class<?> nbtCompound = Class.forName("net.minecraft.server." + version + ".NBTTagCompound");
                return nbtCompound.getDeclaredConstructor().newInstance();
            }
        }
    }

    private static Object createNBTList() throws Exception {
        try {
            Class<?> nbtList = Class.forName("net.minecraft.nbt.NbtList");
            return nbtList.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            try {
                Class<?> nbtList = Class.forName("net.minecraft.nbt.NBTTagList");
                return nbtList.getDeclaredConstructor().newInstance();
            } catch (ClassNotFoundException e2) {
                String version = getServerVersion();
                Class<?> nbtList = Class.forName("net.minecraft.server." + version + ".NBTTagList");
                return nbtList.getDeclaredConstructor().newInstance();
            }
        }
    }

    private static Object createNBTString(String value) throws Exception {
        try {
            Class<?> nbtString = Class.forName("net.minecraft.nbt.NbtString");
            try {
                Method create = nbtString.getMethod("of", String.class);
                return create.invoke(null, value);
            } catch (NoSuchMethodException e) {
                Method create = nbtString.getMethod("a", String.class);
                return create.invoke(null, value);
            }
        } catch (ClassNotFoundException e) {
            try {
                Class<?> nbtString = Class.forName("net.minecraft.nbt.NBTTagString");
                try {
                    Method create = nbtString.getMethod("a", String.class);
                    return create.invoke(null, value);
                } catch (NoSuchMethodException ex) {
                    return nbtString.getConstructor(String.class).newInstance(value);
                }
            } catch (ClassNotFoundException e2) {
                String version = getServerVersion();
                Class<?> nbtString = Class.forName("net.minecraft.server." + version + ".NBTTagString");
                return nbtString.getConstructor(String.class).newInstance(value);
            }
        }
    }

    private static Object getNBTList(Object compound, String key) throws Exception {
        Class<?> compoundClass = compound.getClass();

        if (DEBUG) {
            System.out.println("[NBT DEBUG] ===== ATTEMPTING TO GET LIST: " + key + " =====");
        }

        Exception lastException = null;

        try {
            Method getList = compoundClass.getMethod("getList", String.class, int.class);
            Object result = getList.invoke(compound, key, 10);

            if (result != null) {
                int size = getNBTListSize(result);
                if (DEBUG) {
                    System.out.println("[NBT DEBUG] ✓ getList(String, int) succeeded!");
                    System.out.println("[NBT DEBUG]   Result class: " + result.getClass().getName());
                    System.out.println("[NBT DEBUG]   List size: " + size);
                }
                return result;
            }
        } catch (NoSuchMethodException e) {
            if (DEBUG) System.out.println("[NBT DEBUG] getList(String, int) method not found");
            lastException = e;
        } catch (Exception e) {
            if (DEBUG) System.out.println("[NBT DEBUG] getList(String, int) failed: " + e.getMessage());
            lastException = e;
        }

        try {
            Method get = compoundClass.getMethod("get", String.class);
            Object result = get.invoke(compound, key);

            if (result != null) {
                String className = result.getClass().getName().toLowerCase();
                if (className.contains("list")) {
                    int size = getNBTListSize(result);
                    if (DEBUG) {
                        System.out.println("[NBT DEBUG] ✓ get(String) returned a list!");
                        System.out.println("[NBT DEBUG]   Result class: " + result.getClass().getName());
                        System.out.println("[NBT DEBUG]   List size: " + size);
                    }
                    return result;
                }
            }
        } catch (Exception e) {
            if (DEBUG) System.out.println("[NBT DEBUG] get(String) failed: " + e.getMessage());
            lastException = e;
        }

        try {
            Method getList = compoundClass.getMethod("getList", String.class);
            Object optional = getList.invoke(compound, key);

            if (optional != null) {
                Class<?> optionalClass = optional.getClass();
                Method isPresent = optionalClass.getMethod("isPresent");
                if ((Boolean) isPresent.invoke(optional)) {
                    Method get = optionalClass.getMethod("get");
                    Object result = get.invoke(optional);

                    int size = getNBTListSize(result);
                    if (DEBUG) {
                        System.out.println("[NBT DEBUG] ✓ getList(String) with Optional succeeded!");
                        System.out.println("[NBT DEBUG]   Result class: " + result.getClass().getName());
                        System.out.println("[NBT DEBUG]   List size: " + size);
                    }
                    return result;
                }
            }
        } catch (Exception e) {
            if (DEBUG) System.out.println("[NBT DEBUG] getList(String) Optional approach failed: " + e.getMessage());
            lastException = e;
        }

        try {
            Method getListOrEmpty = compoundClass.getMethod("getListOrEmpty", String.class);
            Object result = getListOrEmpty.invoke(compound, key);

            if (result != null) {
                int size = getNBTListSize(result);
                if (DEBUG) {
                    System.out.println("[NBT DEBUG] ⚠ Using getListOrEmpty (last resort)");
                    System.out.println("[NBT DEBUG]   Result class: " + result.getClass().getName());
                    System.out.println("[NBT DEBUG]   List size: " + size);
                }

                if (size > 0) {
                    return result;
                } else {
                    if (DEBUG) System.out.println("[NBT DEBUG] ⚠ getListOrEmpty returned empty list - checking if key exists");

                    try {
                        Method contains = compoundClass.getMethod("contains", String.class);
                        boolean exists = (Boolean) contains.invoke(compound, key);
                        if (!exists) {
                            if (DEBUG) System.out.println("[NBT DEBUG] Key '" + key + "' does not exist in NBT");
                            return result;
                        } else {
                            if (DEBUG) System.out.println("[NBT DEBUG] Key '" + key + "' exists but list is empty");
                            return result;
                        }
                    } catch (Exception e2) {
                        return result;
                    }
                }
            }
        } catch (Exception e) {
            if (DEBUG) System.out.println("[NBT DEBUG] getListOrEmpty failed: " + e.getMessage());
            lastException = e;
        }

        if (DEBUG) {
            System.out.println("[NBT DEBUG] ⚠ Could not find any method that returns a list for key: " + key);
            if (lastException != null) {
                System.out.println("[NBT DEBUG] Last exception: " + lastException.getMessage());
            }
        }

        return null;
    }

    private static void setNBTList(Object compound, String key, Object list) throws Exception {
        Class<?> compoundClass = compound.getClass();

        Class<?> nbtBaseClass = null;
        try {
            nbtBaseClass = Class.forName("net.minecraft.nbt.Tag");
        } catch (ClassNotFoundException e1) {
            try {
                nbtBaseClass = Class.forName("net.minecraft.nbt.NBTBase");
            } catch (ClassNotFoundException e2) {
                try {
                    String version = getServerVersion();
                    nbtBaseClass = Class.forName("net.minecraft.server." + version + ".NBTBase");
                } catch (ClassNotFoundException e3) {
                    throw new Exception("Could not find NBT base class");
                }
            }
        }

        try {
            Method put = compoundClass.getMethod("put", String.class, nbtBaseClass);
            put.invoke(compound, key, list);
            return;
        } catch (NoSuchMethodException e) {
        }

        try {
            Method set = compoundClass.getMethod("set", String.class, nbtBaseClass);
            set.invoke(compound, key, list);
            return;
        } catch (NoSuchMethodException e) {
        }

        try {
            Method put = compoundClass.getDeclaredMethod("put", String.class, nbtBaseClass);
            put.setAccessible(true);
            put.invoke(compound, key, list);
            return;
        } catch (NoSuchMethodException e) {
        }

        try {
            Method set = compoundClass.getDeclaredMethod("set", String.class, nbtBaseClass);
            set.setAccessible(true);
            set.invoke(compound, key, list);
            return;
        } catch (NoSuchMethodException e) {
        }

        try {
            Method set = compoundClass.getMethod("a", String.class, nbtBaseClass);
            set.invoke(compound, key, list);
            return;
        } catch (NoSuchMethodException e) {
        }

        throw new NoSuchMethodException("Could not find put/set method in CompoundTag class");
    }

    private static int getNBTListSize(Object list) throws Exception {
        if (list == null) {
            return 0;
        }

        Class<?> listClass = list.getClass();

        if (listClass.getName().contains("Optional")) {
            try {
                Method isPresent = listClass.getMethod("isPresent");
                if (!(Boolean) isPresent.invoke(list)) {
                    return 0;
                }
                Method get = listClass.getMethod("get");
                list = get.invoke(list);
                listClass = list.getClass();
            } catch (Exception e) {
                return 0;
            }
        }

        try {
            Method size = listClass.getMethod("size");
            Object result = size.invoke(list);
            return ((Number) result).intValue();
        } catch (NoSuchMethodException e) {
        }

        try {
            Method size = listClass.getDeclaredMethod("size");
            size.setAccessible(true);
            Object result = size.invoke(list);
            return ((Number) result).intValue();
        } catch (NoSuchMethodException e) {
        }

        try {
            Method size = listClass.getMethod("d");
            Object result = size.invoke(list);
            return ((Number) result).intValue();
        } catch (NoSuchMethodException e) {
        }

        if (list instanceof java.util.Collection) {
            return ((java.util.Collection<?>) list).size();
        }

        throw new NoSuchMethodException("Could not find size method on NBT list: " + listClass.getName());
    }

    private static Object getNBTListElement(Object list, int index) throws Exception {
        if (list == null) {
            return null;
        }

        Class<?> listClass = list.getClass();

        if (listClass.getName().contains("Optional")) {
            try {
                Method isPresent = listClass.getMethod("isPresent");
                if (!(Boolean) isPresent.invoke(list)) {
                    return null;
                }
                Method get = listClass.getMethod("get");
                list = get.invoke(list);
                listClass = list.getClass();
            } catch (Exception e) {
                return null;
            }
        }

        try {
            Method get = listClass.getMethod("get", int.class);
            return get.invoke(list, index);
        } catch (NoSuchMethodException e) {
        }

        try {
            Method get = listClass.getDeclaredMethod("get", int.class);
            get.setAccessible(true);
            return get.invoke(list, index);
        } catch (NoSuchMethodException e) {
        }

        try {
            Method get = listClass.getMethod("a", int.class);
            return get.invoke(list, index);
        } catch (NoSuchMethodException e) {
        }

        try {
            Method getCompound = listClass.getMethod("getCompound", int.class);
            return getCompound.invoke(list, index);
        } catch (NoSuchMethodException e) {
        }

        throw new NoSuchMethodException("Could not find get method on NBT list: " + listClass.getName());
    }

    private static void addToNBTList(Object list, Object element) throws Exception {
        if (list == null || element == null) {
            return;
        }

        Class<?> listClass = list.getClass();

        Class<?> nbtBaseClass = null;
        try {
            nbtBaseClass = Class.forName("net.minecraft.nbt.Tag");
        } catch (ClassNotFoundException e1) {
            try {
                nbtBaseClass = Class.forName("net.minecraft.nbt.NBTBase");
            } catch (ClassNotFoundException e2) {
                String version = getServerVersion();
                nbtBaseClass = Class.forName("net.minecraft.server." + version + ".NBTBase");
            }
        }

        try {
            Method add = listClass.getMethod("add", nbtBaseClass);
            add.invoke(list, element);
            return;
        } catch (NoSuchMethodException e) {
        }

        try {
            Method add = listClass.getMethod("add", int.class, nbtBaseClass);
            int size = getNBTListSize(list);
            add.invoke(list, size, element);
            return;
        } catch (NoSuchMethodException e) {
        }

        try {
            Method add = listClass.getDeclaredMethod("add", nbtBaseClass);
            add.setAccessible(true);
            add.invoke(list, element);
            return;
        } catch (NoSuchMethodException e) {
        }

        try {
            Method add = listClass.getMethod("a", nbtBaseClass);
            add.invoke(list, element);
            return;
        } catch (NoSuchMethodException e) {
        }

        throw new NoSuchMethodException("Could not find add method on NBT list: " + listClass.getName());
    }

    private static String getNBTString(Object compound, String key) throws Exception {
        try {
            Method getString = compound.getClass().getMethod("getString", String.class);
            Object result = getString.invoke(compound, key);

            if (result != null && result.getClass().getName().contains("Optional")) {
                Method isPresent = result.getClass().getMethod("isPresent");
                if ((Boolean) isPresent.invoke(result)) {
                    Method get = result.getClass().getMethod("get");
                    return (String) get.invoke(result);
                }
                return "";
            }

            return (String) result;
        } catch (Exception e) {
            return "";
        }
    }

    private static void setNBTString(Object compound, String key, String value) throws Exception {
        Class<?> compoundClass = compound.getClass();

        try {
            Method putString = compoundClass.getMethod("putString", String.class, String.class);
            putString.invoke(compound, key, value);
            return;
        } catch (NoSuchMethodException e) {
        }

        try {
            Method setString = compoundClass.getMethod("setString", String.class, String.class);
            setString.invoke(compound, key, value);
            return;
        } catch (NoSuchMethodException e) {
        }

        try {
            Object nbtString = createNBTString(value);
            Class<?> nbtBaseClass = getNBTBaseClassObject();
            Method put = compoundClass.getMethod("put", String.class, nbtBaseClass);
            put.invoke(compound, key, nbtString);
            return;
        } catch (Exception e) {
        }

        throw new NoSuchMethodException("Could not find putString/setString method in CompoundTag class");
    }

    private static byte getNBTByte(Object compound, String key) throws Exception {
        try {
            Method getByte = compound.getClass().getMethod("getByte", String.class);
            Object result = getByte.invoke(compound, key);

            if (result != null && result.getClass().getName().contains("Optional")) {
                Method isPresent = result.getClass().getMethod("isPresent");
                if ((Boolean) isPresent.invoke(result)) {
                    Method get = result.getClass().getMethod("get");
                    return ((Number) get.invoke(result)).byteValue();
                }
                return 0;
            }

            return ((Number) result).byteValue();
        } catch (Exception e) {
            if (DEBUG) System.out.println("[NBT DEBUG] Error getting byte for key '" + key + "': " + e.getMessage());
            return 0;
        }
    }

    private static void setNBTByte(Object compound, String key, byte value) throws Exception {
        Class<?> compoundClass = compound.getClass();

        try {
            Method putByte = compoundClass.getMethod("putByte", String.class, byte.class);
            putByte.invoke(compound, key, value);
            return;
        } catch (NoSuchMethodException e) {
        }

        try {
            Method setByte = compoundClass.getMethod("setByte", String.class, byte.class);
            setByte.invoke(compound, key, value);
            return;
        } catch (NoSuchMethodException e) {
        }

        throw new NoSuchMethodException("Could not find putByte/setByte method in CompoundTag class");
    }

    private static short getNBTShort(Object compound, String key) throws Exception {
        try {
            Method getShort = compound.getClass().getMethod("getShort", String.class);
            Object result = getShort.invoke(compound, key);

            if (result != null && result.getClass().getName().contains("Optional")) {
                Method isPresent = result.getClass().getMethod("isPresent");
                if ((Boolean) isPresent.invoke(result)) {
                    Method get = result.getClass().getMethod("get");
                    return ((Number) get.invoke(result)).shortValue();
                }
                return 0;
            }

            return ((Number) result).shortValue();
        } catch (Exception e) {
            return 0;
        }
    }

    private static Class<?> getNBTBaseClassObject() throws ClassNotFoundException {
        try {
            return Class.forName("net.minecraft.nbt.Tag");
        } catch (ClassNotFoundException e) {
            try {
                return Class.forName("net.minecraft.nbt.NBTBase");
            } catch (ClassNotFoundException e2) {
                String version = getServerVersion();
                return Class.forName("net.minecraft.server." + version + ".NBTBase");
            }
        }
    }

    private static void setNBTShort(Object compound, String key, short value) throws Exception {
        Class<?> compoundClass = compound.getClass();

        try {
            Method putShort = compoundClass.getMethod("putShort", String.class, short.class);
            putShort.invoke(compound, key, value);
            return;
        } catch (NoSuchMethodException e) {
        }

        try {
            Method setShort = compoundClass.getMethod("setShort", String.class, short.class);
            setShort.invoke(compound, key, value);
            return;
        } catch (NoSuchMethodException e) {
        }

        throw new NoSuchMethodException("Could not find putShort/setShort method in CompoundTag class");
    }

    private static Object getNBTCompound(Object compound, String key) throws Exception {
        try {
            Method getCompound = compound.getClass().getMethod("getCompound", String.class);
            return getCompound.invoke(compound, key);
        } catch (Exception e) {
            return null;
        }
    }

    private static void setNBTCompound(Object compound, String key, Object value) throws Exception {
        Class<?> compoundClass = compound.getClass();

        Class<?> nbtBaseClass = null;
        try {
            nbtBaseClass = Class.forName("net.minecraft.nbt.Tag");
        } catch (ClassNotFoundException e1) {
            try {
                nbtBaseClass = Class.forName("net.minecraft.nbt.NBTBase");
            } catch (ClassNotFoundException e2) {
                String version = getServerVersion();
                nbtBaseClass = Class.forName("net.minecraft.server." + version + ".NBTBase");
            }
        }

        try {
            Method put = compoundClass.getMethod("put", String.class, nbtBaseClass);
            put.invoke(compound, key, value);
            return;
        } catch (NoSuchMethodException e) {
        }

        try {
            Method set = compoundClass.getMethod("set", String.class, nbtBaseClass);
            set.invoke(compound, key, value);
            return;
        } catch (NoSuchMethodException e) {
        }

        throw new NoSuchMethodException("Could not find put/set method in CompoundTag class");
    }


    private static String getServerVersion() {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        return packageName.substring(packageName.lastIndexOf('.') + 1);
    }
}