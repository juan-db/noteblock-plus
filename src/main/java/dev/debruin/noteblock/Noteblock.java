package dev.debruin.noteblock;

import net.minecraft.block.NoteBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.NoteBlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

@Mod("noteblock")
public class Noteblock {
    public Noteblock() {
        // Make sure the mod being absent on the other network side does not cause the client to
        // display the server as incompatible
        ModLoadingContext.get().registerExtensionPoint(
            ExtensionPoint.DISPLAYTEST,
            () -> Pair.of(
                () -> FMLNetworkConstants.IGNORESERVERONLY,
                (a, b) -> true
            )
        );

        MinecraftForge.EVENT_BUS.register(this);
    }

    /** Note blocks that have been shift-clicked recently. */
    private final List<BlockPos> shiftClickedNoteBlocks = new ArrayList<>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockUsed(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isRemote) {
            return;
        }

    	if (!(event.getWorld().getBlockState(event.getPos()).getBlock() instanceof NoteBlock)) {
            return;
        }

        if (!event.getPlayer().isSneaking()) {
            return;
        }

        if (event.getItemStack() != ItemStack.EMPTY) {
            return;
        }

        shiftClickedNoteBlocks.add(event.getPos());
    }

    @SubscribeEvent
    public void onNoteChanged(NoteBlockEvent.Change event) {
        if (event.getWorld().isRemote()) {
            return;
        }

        if (shiftClickedNoteBlocks.remove(event.getPos())) {
            // I believe the vanilla note ID represents the new note, not the old one, so we have
            // to subtract one for the already added one, then another to go back a note.
            int newNote = event.getVanillaNoteId() - 2;
            if (newNote < 0) {
                newNote = 25 + newNote;
            }

            event.setNote(noteFromId(newNote), octaveFromId(newNote));
        }
    }

    /**
     * @param id the vanilla note ID
     * @return the octave the ID falls within
     * @see Noteblock#noteFromId(int)
     */
    private static NoteBlockEvent.Octave octaveFromId(int id) {
        // Basically just the fromId function from the Octave enum, because it's package private there.
        if (id < 12) {
            return NoteBlockEvent.Octave.LOW;
        } else if (id < 24) {
            return NoteBlockEvent.Octave.MID;
        } else {
            return NoteBlockEvent.Octave.HIGH;
        }
    }


    /**
     * @param id the vanilla note ID
     * @return the note that corresponds to the given ID
     * @see Noteblock#octaveFromId(int)
     */
    private static NoteBlockEvent.Note noteFromId(int id) {
        // The same as the fromId function from the Note enum, because it's package private there.
        return NoteBlockEvent.Note.values()[id % 12];
    }
}
