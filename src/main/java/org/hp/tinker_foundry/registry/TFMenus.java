package org.hp.tinker_foundry.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.RegistryObject;
import org.hp.tinker_foundry.TinkerFoundry;
import org.hp.tinker_foundry.menu.FoundryMenu;

/** 冶炼系统菜单注册表。 */
public final class TFMenus {
    /** 所有冶炼设备共用的服务端权威菜单。 */
    public static final RegistryObject<MenuType<FoundryMenu>> FOUNDRY = TinkerFoundry.MENUS.register(
        "foundry",
        // 使用 Forge 菜单载荷把设备位置传给客户端，确保不同设备使用各自的槽位布局。
        () -> IForgeMenuType.create(FoundryMenu::new)
    );

    private TFMenus() {
    }
}
