package com.journeymode.research;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.OminousBottleAmplifier;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * What a piece of research is saved under. Most items are researched as just the item
 * ({@code minecraft:oak_log}). Some items come in kinds that are different things to research, so
 * the kind is added after a {@code #}:
 * <ul>
 *   <li>potions, splash and lingering potions and tipped arrows, by potion
 *   ({@code minecraft:potion#minecraft:strong_healing});</li>
 *   <li>enchanted books, by their enchantments and levels
 *   ({@code minecraft:enchanted_book#minecraft:sharpness@5});</li>
 *   <li>ominous bottles, by strength ({@code minecraft:ominous_bottle#2}).</li>
 * </ul>
 * Those items are never researched as just the item, since that would be a potion with no potion in it.
 */
public final class ResearchKeys {
	private static final char KIND = '#';

	private ResearchKeys() {
	}

	/** The key the stack is researched under. */
	public static String of(ItemStack stack) {
		String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		String kind = kind(stack);
		return kind == null ? id : id + KIND + kind;
	}

	/** The item a key is about, or null if it is not an item that exists now (a removed mod's, say). */
	public static Item itemOf(String key) {
		int at = key.indexOf(KIND);
		Identifier id = Identifier.tryParse(at < 0 ? key : key.substring(0, at));
		Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
		return item == null || item == Items.AIR ? null : item;
	}

	/** Whether the item is one of those that are researched by kind and never as just the item. */
	private static boolean byKind(Item item) {
		return item == Items.ENCHANTED_BOOK || item == Items.OMINOUS_BOTTLE || item == Items.POTION
				|| item == Items.SPLASH_POTION || item == Items.LINGERING_POTION || item == Items.TIPPED_ARROW;
	}

	/** Which kind of its item the stack is, or null if it has none. */
	private static String kind(ItemStack stack) {
		Item item = stack.getItem();
		if (item == Items.ENCHANTED_BOOK) {
			ItemEnchantments enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
			if (enchantments == null || enchantments.isEmpty()) {
				return null;
			}
			List<String> parts = new ArrayList<>();
			for (var entry : enchantments.entrySet()) {
				parts.add(entry.getKey().getRegisteredName() + "@" + entry.getIntValue());
			}
			parts.sort(Comparator.naturalOrder());
			return String.join(",", parts);
		}
		if (item == Items.OMINOUS_BOTTLE) {
			OminousBottleAmplifier amplifier = stack.get(DataComponents.OMINOUS_BOTTLE_AMPLIFIER);
			return amplifier == null ? null : String.valueOf(amplifier.value());
		}
		if (byKind(item)) {
			PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
			return contents == null ? null : contents.potion().map(Holder::getRegisteredName).orElse(null);
		}
		return null;
	}

	/**
	 * A single item of the kind a key describes, or an empty stack if the key is not something that can be
	 * made now (the item or the enchantment or potion no longer exists).
	 */
	public static ItemStack stackOf(String key, HolderLookup.Provider registries) {
		Item item = itemOf(key);
		if (item == null) {
			return ItemStack.EMPTY;
		}
		int at = key.indexOf(KIND);
		if (at < 0) {
			return byKind(item) ? ItemStack.EMPTY : new ItemStack(item);
		}
		String kind = key.substring(at + 1);
		ItemStack stack = new ItemStack(item);
		try {
			if (item == Items.ENCHANTED_BOOK) {
				ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
				for (String part : kind.split(",")) {
					int split = part.lastIndexOf('@');
					Identifier id = Identifier.tryParse(part.substring(0, split));
					Optional<Holder.Reference<Enchantment>> enchantment = id == null ? Optional.empty()
							: registries.lookupOrThrow(Registries.ENCHANTMENT).get(ResourceKey.create(Registries.ENCHANTMENT, id));
					if (enchantment.isEmpty()) {
						return ItemStack.EMPTY;
					}
					enchantments.set(enchantment.get(), Integer.parseInt(part.substring(split + 1)));
				}
				stack.set(DataComponents.STORED_ENCHANTMENTS, enchantments.toImmutable());
			} else if (item == Items.OMINOUS_BOTTLE) {
				stack.set(DataComponents.OMINOUS_BOTTLE_AMPLIFIER, new OminousBottleAmplifier(Integer.parseInt(kind)));
			} else {
				Identifier id = Identifier.tryParse(kind);
				Optional<Holder.Reference<Potion>> potion = id == null ? Optional.empty() : BuiltInRegistries.POTION.get(id);
				if (potion.isEmpty()) {
					return ItemStack.EMPTY;
				}
				stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion.get()));
			}
		} catch (RuntimeException e) {
			return ItemStack.EMPTY;
		}
		return stack;
	}
}
