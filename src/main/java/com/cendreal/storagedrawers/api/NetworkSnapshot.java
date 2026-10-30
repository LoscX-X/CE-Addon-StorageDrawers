package com.cendreal.storagedrawers.api;
import java.util.List;
public record NetworkSnapshot(StoragePosition controller,List<StoragePosition> drawers,long revision){public NetworkSnapshot{drawers=List.copyOf(drawers);}}
