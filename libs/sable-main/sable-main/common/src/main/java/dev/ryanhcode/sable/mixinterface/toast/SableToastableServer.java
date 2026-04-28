package dev.ryanhcode.sable.mixinterface.toast;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;

public interface SableToastableServer {
    void sable$reportSubLevelLoadFailure(GlobalSavedSubLevelPointer data);

    void sable$reportSubLevelSaveFailure(SubLevelData data);

    void sable$reportSubLevelPhysicsFailure(ServerSubLevel data);
}
