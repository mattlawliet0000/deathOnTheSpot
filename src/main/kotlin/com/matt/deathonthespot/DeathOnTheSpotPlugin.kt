package com.matt.deathonthespot

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Chest
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.*

class DeathOnTheSpotPlugin : JavaPlugin(), Listener {

    private val objectMapper = ObjectMapper().registerModule(KotlinModule())
    private lateinit var customConfig: org.bukkit.configuration.file.FileConfiguration
    private lateinit var configFile: File
    private lateinit var deathDataDir: File

    override fun onEnable() {
        server.pluginManager.registerEvents(this, this)

        // Create config directory
        val configDir = File("config/DeathOnTheSpot")
        if (!configDir.exists()) {
            configDir.mkdirs()
        }

        // Setup custom config
        configFile = File(configDir, "config.yml")
        if (!configFile.exists()) {
            saveResource("config.yml", false)
            val defaultConfig = File(dataFolder, "config.yml")
            if (defaultConfig.exists()) {
                defaultConfig.copyTo(configFile, overwrite = true)
            }
        }
        customConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile)

        // Create death data directory
        deathDataDir = File(configDir, "death_data")
        if (!deathDataDir.exists()) {
            deathDataDir.mkdirs()
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (command.name.equals("setdeathchest", ignoreCase = true)) {
            if (sender !is Player) {
                sender.sendMessage("This command can only be run by a player.")
                return true
            }
            if (!sender.hasPermission("deathonthespot.setchest")) {
                sender.sendMessage("You don't have permission to use this command.")
                return true
            }
            val targetedBlock = sender.getTargetBlock(null, 5) // Look up to 5 blocks ahead
            if (targetedBlock.type != Material.CHEST) {
                sender.sendMessage("You must be looking at a chest to set the death chest location.")
                return true
            }
            val loc = targetedBlock.location
            customConfig.set("chest.world", loc.world.name)
            customConfig.set("chest.x", loc.blockX)
            customConfig.set("chest.y", loc.blockY)
            customConfig.set("chest.z", loc.blockZ)
            customConfig.save(configFile)
            sender.sendMessage("Death chest location set to ${loc.blockX}, ${loc.blockY}, ${loc.blockZ} in ${loc.world.name}")
            return true
        }
        return false
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        val inventory = player.inventory.contents
        val itemsMap = mutableMapOf<Int, ItemData>()

        inventory.forEachIndexed { index, item ->
            if (item != null) {
                itemsMap[index] = ItemData(item.serialize())
            }
        }

        if (itemsMap.isNotEmpty()) {
            val inventoryData = InventoryData(itemsMap)
            val file = File(deathDataDir, "${player.uniqueId}.json")
            objectMapper.writeValue(file, inventoryData)
            event.drops.clear() // Prevent items from dropping naturally
            player.sendMessage("Your inventory has been saved! Right-click the death chest to retrieve it.")
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        if (block.type != Material.CHEST) return
        val chestLoc = block.location
        val configWorld = customConfig.getString("chest.world")
        val configX = customConfig.getInt("chest.x")
        val configY = customConfig.getInt("chest.y")
        val configZ = customConfig.getInt("chest.z")
        if (chestLoc.world?.name != configWorld || chestLoc.blockX != configX || chestLoc.blockY != configY || chestLoc.blockZ != configZ) return

        // Cancel the default chest opening to prevent GUI conflicts
        event.setCancelled(true)

        val player = event.player
        val file = File(deathDataDir, "${player.uniqueId}.json")
        if (!file.exists()) {
            player.sendMessage("No saved inventory found.")
            return
        }

        try {
            val inventoryData: InventoryData = objectMapper.readValue(file, InventoryData::class.java)
            logger.info("Loaded ${inventoryData.items.size} items for player ${player.name}")

            val virtualInv = Bukkit.createInventory(null, 54, "Death Inventory")
            var itemsLoaded = 0

            inventoryData.items.forEach { (slot, itemData) ->
                try {
                    val item = ItemStack.deserialize(itemData.serializedData)
                    virtualInv.setItem(slot, item)
                    itemsLoaded++
                } catch (e: Exception) {
                    logger.warning("Failed to deserialize item at slot $slot: ${e.message}")
                }
            }

            player.sendMessage("Loaded $itemsLoaded items from your saved inventory.")
            player.openInventory(virtualInv)

            // Store the inventory and file for later identification and removal
            player.setMetadata("deathInventory", org.bukkit.metadata.FixedMetadataValue(this, virtualInv))
            player.setMetadata("deathInventoryFile", org.bukkit.metadata.FixedMetadataValue(this, file))
        } catch (e: Exception) {
            logger.severe("Error loading inventory for player ${player.name}: ${e.message}")
            player.sendMessage("Error loading your saved inventory. Please contact an administrator.")
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val deathInventory = player.getMetadata("deathInventory").firstOrNull()?.value() as? org.bukkit.inventory.Inventory ?: return

        // Check if this involves the death inventory GUI
        if (event.inventory == deathInventory || event.clickedInventory == deathInventory) {

            // Prevent adding items TO the death inventory (raw slots 0-53)
            if (event.rawSlot < 54 &&
                (event.action == org.bukkit.event.inventory.InventoryAction.PLACE_ALL ||
                 event.action == org.bukkit.event.inventory.InventoryAction.PLACE_SOME ||
                 event.action == org.bukkit.event.inventory.InventoryAction.PLACE_ONE ||
                 event.action == org.bukkit.event.inventory.InventoryAction.SWAP_WITH_CURSOR)) {
                event.setCancelled(true)
                player.sendMessage("§cYou can only take items from the death chest!")
                return
            }

            // Allow taking items FROM the death inventory (raw slots 0-53) - Direct clicks only
            if (event.clickedInventory == deathInventory &&
                event.rawSlot < 54 &&
                event.action != org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY &&
                event.action != org.bukkit.event.inventory.InventoryAction.COLLECT_TO_CURSOR &&
                event.action != org.bukkit.event.inventory.InventoryAction.HOTBAR_MOVE_AND_READD &&
                event.action != org.bukkit.event.inventory.InventoryAction.HOTBAR_SWAP) {
                val file = player.getMetadata("deathInventoryFile").firstOrNull()?.value() as? File ?: return
                try {
                    val inventoryData: InventoryData = objectMapper.readValue(file, InventoryData::class.java)
                    val clickedSlot = event.slot

                    if (inventoryData.items.containsKey(clickedSlot)) {
                        val itemData = inventoryData.items[clickedSlot]!!
                        val item = ItemStack.deserialize(itemData.serializedData)

                        // Cancel the event and set the cursor to the item
                        event.setCancelled(true)
                        event.setCursor(item)

                        // Remove item from data
                        inventoryData.items.remove(clickedSlot)
                        objectMapper.writeValue(file, inventoryData)

                        // Clear the slot in the GUI
                        deathInventory.setItem(clickedSlot, null)

                        if (inventoryData.items.isEmpty()) {
                            file.delete()
                            player.removeMetadata("deathInventoryFile", this)
                        }
                    }
                } catch (e: Exception) {
                    logger.severe("Error updating inventory file for player ${player.name}: ${e.message}")
                    player.sendMessage("§cError saving inventory changes. Please contact an administrator.")
                }
            }

            // Handle shift-clicking and other bulk moves FROM death inventory
            if (event.clickedInventory == deathInventory &&
                event.rawSlot < 54 &&
                (event.action == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY ||
                 event.action == org.bukkit.event.inventory.InventoryAction.COLLECT_TO_CURSOR ||
                 event.action == org.bukkit.event.inventory.InventoryAction.HOTBAR_MOVE_AND_READD ||
                 event.action == org.bukkit.event.inventory.InventoryAction.HOTBAR_SWAP)) {

                val file = player.getMetadata("deathInventoryFile").firstOrNull()?.value() as? File ?: return
                try {
                    val inventoryData: InventoryData = objectMapper.readValue(file, InventoryData::class.java)
                    val clickedSlot = event.slot

                    // Remove item from the specific slot for bulk operations
                    if (inventoryData.items.containsKey(clickedSlot)) {
                        inventoryData.items.remove(clickedSlot)
                        objectMapper.writeValue(file, inventoryData)

                        if (inventoryData.items.isEmpty()) {
                            file.delete()
                            player.removeMetadata("deathInventoryFile", this)
                        }
                    }
                } catch (e: Exception) {
                    logger.severe("Error updating inventory file for player ${player.name}: ${e.message}")
                    player.sendMessage("§cError saving inventory changes. Please contact an administrator.")
                }
            }

            // Prevent shift-clicking items FROM personal inventory TO death inventory
            if (event.clickedInventory != deathInventory && event.action == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true)
                player.sendMessage("§cYou cannot shift-click items into the death chest!")
            }
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val deathInventory = player.getMetadata("deathInventory").firstOrNull()?.value() as? org.bukkit.inventory.Inventory ?: return
        if (event.inventory == deathInventory) {
            player.removeMetadata("deathInventory", this)
            player.removeMetadata("deathInventoryFile", this)
        }
    }
}

data class InventoryData(val items: MutableMap<Int, ItemData>)
data class ItemData(val serializedData: Map<String, Any>)