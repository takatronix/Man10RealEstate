package red.man10.realestate.region.user

import org.bukkit.Material

enum class Permission {
    ALL,
    BLOCK,
    DOOR,
    INVENTORY;


    companion object {

        private val interactiveBlockMap=HashMap<Permission,List<Material>>()

        fun getAllowedBlocks(permission: Permission):List<Material>{
            return interactiveBlockMap[permission]?: listOf()
        }

        init {

            interactiveBlockMap[INVENTORY]= listOf(
                    Material.CHEST,
                    Material.ENDER_CHEST,
                    Material.HOPPER,
                    Material.TRAPPED_CHEST,
                    Material.DISPENSER,
                    Material.DROPPER,
                    Material.BLAST_FURNACE,
                    Material.FURNACE,
                    Material.BARREL,
                    Material.SHULKER_BOX,
                    Material.BLACK_SHULKER_BOX,
                    Material.BLUE_SHULKER_BOX,
                    Material.BROWN_SHULKER_BOX,
                    Material.CYAN_SHULKER_BOX,
                    Material.GRAY_SHULKER_BOX,
                    Material.GREEN_SHULKER_BOX,
                    Material.LIGHT_BLUE_SHULKER_BOX,
                    Material.LIGHT_GRAY_SHULKER_BOX,
                    Material.LIME_SHULKER_BOX,
                    Material.MAGENTA_SHULKER_BOX,
                    Material.ORANGE_SHULKER_BOX,
                    Material.PINK_SHULKER_BOX,
                    Material.PURPLE_SHULKER_BOX,
                    Material.RED_SHULKER_BOX,
                    Material.WHITE_SHULKER_BOX,
                    Material.YELLOW_SHULKER_BOX)

            interactiveBlockMap[DOOR]= listOf(
                    Material.ACACIA_DOOR,
                    Material.BAMBOO_DOOR,
                    Material.DARK_OAK_DOOR,
                    Material.BIRCH_DOOR,
                    Material.CHERRY_DOOR,
                    Material.CRIMSON_DOOR,
                    Material.JUNGLE_DOOR,
                    Material.MANGROVE_DOOR,
                    Material.OAK_DOOR,
                    Material.SPRUCE_DOOR,
                    Material.SPRUCE_DOOR,
                    Material.WARPED_DOOR,

                    Material.ACACIA_TRAPDOOR,
                    Material.BAMBOO_TRAPDOOR,
                    Material.DARK_OAK_TRAPDOOR,
                    Material.BIRCH_TRAPDOOR,
                    Material.CHERRY_TRAPDOOR,
                    Material.CRIMSON_TRAPDOOR,
                    Material.JUNGLE_TRAPDOOR,
                    Material.MANGROVE_TRAPDOOR,
                    Material.OAK_TRAPDOOR,
                    Material.SPRUCE_TRAPDOOR,
                    Material.SPRUCE_TRAPDOOR,
                    Material.WARPED_TRAPDOOR,

            )

        }


    }

}