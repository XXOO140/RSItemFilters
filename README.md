# RS Item Filters

为 Refined Storage（精致存储）Grid 终端添加 **Just Enough Filters 风格**的物品分类筛选按钮，让 Grid 也能像 JEI 里那样一键筛出「所有食物 / 所有工具 / 所有装备 / 附魔书 / 燃料 / 方块 / 物品」。

## 功能

在精致存储 Grid（合成终端 / 普通 Grid / 样板终端等）顶部搜索框下方，新增一排图标按钮：

| 按钮 | 图标 | 判定逻辑（与 JEF 源码一致） |
|---|---|---|
| 食物 | 苹果 | `#c:foods` 标签 **或** 含 FOOD 数据组件 |
| 工具 | 钻石镐 | 含 TOOL 组件 **或** `#c:tools` **或**（有耐久 + 不可堆叠 + 可附魔 + 非护甲槽） |
| 装备 | 钻石胸甲 | `#c:armors/humanoid` **或**（有耐久 + 人形护甲槽） |
| 附魔书 | 附魔书 | 是附魔书 |
| 燃料 | 煤炭 | `level.fuelValues().isFuel()` |
| 方块 | 草方块 | 是 BlockItem |
| 物品 | 铁锭 | 不是 BlockItem |

点击按钮切换筛选，再点一次取消。筛选与 Grid 搜索框是 **AND 关系**（可组合）。

## 版本要求

- Minecraft **1.20.1**
- Forge **47+**
- Refined Storage **1.12.4**（要求 `[1.12.4, 1.13.0)`）
- 纯客户端 mod（`side = CLIENT`）

## 编译

需要 JDK 17+ 和网络（首次编译会下载 Forge / Minecraft 反编译环境）。

```bash
# 若已有 gradle wrapper
./gradlew build        # Linux / macOS
gradlew.bat build      # Windows

# 或用本机 gradle
gradle build
```

产物在 `build/libs/rsitemfilters-1.0.0.jar`，放入整合包 `mods/` 文件夹即可。

> 若下载依赖慢，可给 `settings.gradle` / `build.gradle` 追加国内 Maven 镜像。

## 实现说明

- 通过 Mixin 注入 `GridScreen.onPostInit` 添加按钮，注入 `GridViewImpl.getActiveFilters` 叠加分类过滤 Predicate。
- 判定逻辑逐行移植自 [JustEnoughFilters](https://github.com/pSUNSET/JustEnoughFilters) 的 `ToggledFilters.kt`。
- 分类状态为客户端共享，切换分类后调用 `forceSort()` 即时刷新。
