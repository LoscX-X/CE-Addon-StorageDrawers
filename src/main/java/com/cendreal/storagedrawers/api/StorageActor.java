package com.cendreal.storagedrawers.api;
import java.util.UUID;
/** uuid is null for server automation. name is suitable for an audit logger. */
public record StorageActor(UUID uuid,String name,StorageCause cause){}
