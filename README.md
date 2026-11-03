# RS Item Filters

[![Forge](https://img.shields.io/badge/Forge-1.20.1--47.3.33-orange)](https://files.minecraftforge.net/)
[![Refined Storage](https://img.shields.io/badge/Refined%20Storage-1.12.4-blue)](https://www.curseforge.com/minecraft/mc-mods/refined-storage)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

为 **Refined Storage（精致存储）** 的 Grid 终端添加 **Just Enough Filters（JEF）风格** 的物品分类筛选按钮，让你在 RS 终端里也能像在 JEI 里那样，**一键筛出「所有食物 / 所有工具 / 所有装备 / 附魔书 / 燃料 / 方块 / 物品」**。

> 灵感来自 [JustEnoughFilters](https://github.com/pSUNSET/JustEnoughFilters)。原版 JEF 只作用于 JEI/REI/EMI，本模组把它的分类过滤能力移植到了精致存储终端。

---

## ✨ 功能

在精致存储 Grid（合成终端 / 普通 Grid / 样板终端等）**搜索框下方**新增一排图标按钮：

| 按钮 | 图标 | 判定逻辑（与 JEF 源码 `ToggledFilters.kt` 一致） |
|---|---|---|
| 🍎 食物 | 苹果 | `#c:foods` 标签 **或** 含 `FOOD` 数据组件 |
| ⛏️ 工具 | 钻石镐 | 含 `TOOL` 组件 **或** `#c:tools` **或**（有耐久 + 不可堆叠 + 可附魔 + 非护甲槽） |
| 🛡️ 装备 | 钻石胸甲 | `#c:armors/humanoid` **或**（有耐久 + 人形护甲槽） |
| 📖 附魔书 | 附魔书 | 是附魔书 |
| 🔥 燃料 | 煤炭 | `level.fuelValues().isFuel()` |
| 🧱 方块 | 草方块 | 是 `BlockItem` |
| 📦 物品 | 铁锭 | 不是 `BlockItem` |

- **点击按钮切换筛选**，再点一次取消。
- 分类筛选与 Grid 搜索框是 **AND 关系**，可自由组合（例如：先点「食物」按钮，再在搜索框输入 `@farmersdelight`，即可筛出农夫乐事的食物）。
- 判定采用 **「标签 + 数据组件」双重保险**，比单纯依赖 `#c:foods` 标签的覆盖率更高——能兜住那些**没有规范登记标签**的小众 mod 物品。

<!-- TODO: 补充实际截图 -->
<!-- ![截图](docs/screenshot.png) -->

---

## 📦 版本要求

| 依赖 | 版本 |
|---|---|
| Minecraft | **1.20.1** |
| Forge | **47.3.33**（47+ 均可） |
| Refined Storage | **1.12.4** |
| JEI | 客户端必需（你有） |

- 纯客户端模组（`side = CLIENT`），服务器无需安装也能用。

---

## 🔧 安装

1. 下载 `build/libs/rsitemfilters-1.0.0.jar`（或 Releases 页面的 jar）。
2. 把它放入整合包的 `mods/` 文件夹。
3. 启动游戏，打开精致存储终端即可看到顶部新增的分类按钮。

---

## 🛠️ 编译

**环境要求**：JDK 17+（推荐 17，21 亦可），需联网（首次编译会下载 ForgeGradle 和 Minecraft 反编译环境，耗时较长）。

```bash
# 使用 gradle wrapper（推荐，会自动下载 Gradle 8.5）
gradlew.bat build      # Windows
./gradlew build        # Linux / macOS

# 或用本机已安装的 gradle
gradle build
```

产物位置：`build/libs/rsitemfilters-1.0.0.jar`

> 💡 **国内加速**：依赖下载慢时，已在 `build.gradle` / `settings.gradle` 中配置了阿里云 Maven 镜像与 CurseMaven。若仍慢，可自行追加其它镜像源。

---

## 📖 使用技巧：RS 搜索语法速查

精致存储的搜索框语法与 JEI **不同**，这是最容易踩坑的地方：

| 前缀 | RS 中的含义 | 示例 |
|---|---|---|
| `@` | 按**模组**过滤 | `@minecraft` |
| `$` | 按**物品标签(tag)**过滤 | `$c:foods` |
| `#` | 按**提示文本(tooltip)**过滤 | `#foods`（搜 tooltip 里的文字，**不是**标签） |
| （无前缀） | 按**名称**过滤 | `iron` |

> ⚠️ **注意**：在 JEI/JEF 里 `#` 是标签搜索，但精致存储里 **`#` 是 tooltip 搜索**，标签搜索要用 **`$`**。
> 所以想在 RS 里搜「所有食物」应该输入 `$c:foods`，而不是 `#c:foods`。
>
> 本模组的存在意义之一，就是让你**不用记这些前缀**——直接点「食物」按钮即可。

---

## 🏗️ 实现说明

- 通过 **Mixin** 注入：
  - `GridScreen.onPostInit` → 在 Grid 顶部渲染分类按钮；
  - `GridViewImpl.getActiveFilters` → 在过滤链尾部叠加分类过滤 `Predicate`。
- 判定逻辑逐行移植自 [JustEnoughFilters](https://github.com/pSUNSET/JustEnoughFilters) 的 `builtin/ToggledFilters.kt`。
- 分类状态为客户端共享（`CategoryState`），切换分类后调用 `forceSort()` 即时刷新。

---

## 🧾 项目结构

```
RSItemFilters/
├── build.gradle                     # ForgeGradle 6 构建配置
├── gradle.properties                # 版本 / 坐标 / mapping 配置
├── settings.gradle
└── src/main/
    ├── java/com/rsitemfilters/
    │   ├── RSItemFilters.java       # 模组主类
    │   ├── filter/
    │   │   ├── ItemCategory.java    # 7 个分类的判定逻辑（核心）
    │   │   └── CategoryState.java   # 当前选中分类状态
    │   └── mixin/
    │       ├── GridScreenMixin.java     # 注入分类按钮
    │       └── GridViewImplMixin.java   # 注入过滤逻辑
    └── resources/
        ├── META-INF/mods.toml
        ├── rsitemfilters.mixins.json
        └── assets/rsitemfilters/lang/
            ├── zh_cn.json
            └── en_us.json
```

---

## ❓ FAQ

**Q：为什么我的 `#c:foods` 在 RS 里搜不出东西？**
A：因为 RS 里 `#` 是 tooltip 搜索，标签搜索要用 `$`。输入 `$c:foods` 才对。或者直接用本模组的「食物」按钮。

**Q：`#c:foods` 能筛出所有 mod 食物吗？**
A：不一定。`c:foods` 标签需要各 mod 主动登记，很多小众 mod 没登记。本模组用「标签 + FOOD 组件」双重判断，覆盖率更高。

**Q：本模组和 JEF 能共存吗？**
A：可以。JEF 作用于 JEI 界面，本模组作用于 RS 终端，互不冲突。

**Q：服务器需要装吗？**
A：不需要，纯客户端模组。

---

## 📄 License

MIT
