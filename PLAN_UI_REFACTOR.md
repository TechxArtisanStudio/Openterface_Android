# Openterface Android 竖屏 UI 重构计划

> 基于 KeyCMD 项目 (`Openterface_KeyCmd_Android`) 的参考实现，对 AOS 项目 (`Openterface_Android`) 竖屏模式下的鼠标界面、键盘/输入 UI 风格和键盘布局进行重构。

---

## 一、问题分析

### 问题 1：竖屏鼠标界面布局不正确

**现状（AOS 项目 `module_portrait_mouse.xml` + `MouseControlStripView`）：**
- 滚动条是 **上滚/下滚两个独立按钮**，夹在 L/M/R 按键中间，布局为水平 5 区：`[ScrollUp][L][M][R][ScrollDown]`
- 滚动按钮仅在最左侧和最右侧，占用空间小
- L/M/R 按键没有震动反馈
- 顶部 gesture guide 区域有 5 个图标说明（左键/右键/中键/上滚/下滚），文字冗余

**KeyCMD 参考实现（`fragment_basic_touchpad.xml` + `BasicPortraitScrollStripView`）：**
- 滚动条是一条 **连续的垂直拖拽条**，位于 touchpad 右侧，占满整个鼠标 UI 的竖直空间
- touchpad 区域（weight=5）和滚动条（weight=1）在同一行水平排列
- L/M/R 三个按键在下方单独一行（weight=2:1:2），使用 Material keycap 风格
- `BasicPortraitScrollStripView` 通过手指拖拽距离累计滚轮事件，每个 wheel step 触发一次 `HapticFeedbackConstants.CLOCK_TICK` 震动

### 问题 2：竖屏 UI 配色和按键风格不统一

**现状（AOS 项目）：**
- 大量硬编码颜色值：`#F5F5F5`（背景）、`#2A2A2A`（gesture guide）、`#333333`（按钮）、`#AAAAAA`（副文字）、`#3700B3`（Send 按钮）
- 没有 Material Design 主题系统，不跟随系统 dark/light 模式
- 按键是简单的纯色圆角矩形 + elevation，没有 KeyCMD 的 keycap 纹理效果
- `ButtonTheme` 和 `button_background` drawable 使用紫色系旧主题

**KeyCMD 参考实现：**
- Material Design 3 主题系统，支持 8 种 accent color family
- `Theme.Material3.DayNight.NoActionBar` 自动适配 light/dark
- 完整的颜色 token 体系：`background_light`、`text_primary`、`text_secondary`、`header_background`、`divider`
- 按键使用 `key_background` drawable（白色底 + 边框 + 圆角）模拟物理 keycap
- touchpad 表面使用 `km_basic_touchpad_surface` drawable（多层纹理效果）

### 问题 3：竖屏键盘布局和功能需要重构

**现状（AOS 项目 `PortraitKeyboardView` + `module_portrait_keyboard.xml`）：**
- `PortraitKeyboardView` 只加载了 `system_button` 和 `function_button` 两个面板
- 没有 QWERTY 字母键盘面板
- 面板切换仅有 System ↔ Function，功能过于简单
- 竖屏键盘缺少完整的输入能力

**KeyCMD 参考实现（`BasicPhysicalKeyboardView` + `BasicKeyboardFragment`）：**
- 完整的 QWERTY 键盘布局（字母行 + 符号行 + 修饰键行 + 功能键行）
- 通过 `inflateKey` 动态创建按键，支持 key preview popup
- 修饰键支持 sticky latch 和 long-press chord 两种模式
- 子模式切换：Keyboard / Touchpad / NumPad（通过 chrome strip tabs）

---

## 二、修改计划

### Phase 1：鼠标界面布局重构

#### Task 1.1：重构 `module_portrait_mouse.xml` 布局

**目标布局结构：**
```
┌──────────────────────────────────┐
│  [TouchPad Area     ] [Scroll   ]│
│  [                   ] [Strip   ]│
│  [                   ] [(full   ]│
│  [                   ] [ height)]│
│  [                   ] [        ]│
├──────────────────────────────────┤
│  [    L    ] [  M  ] [    R    ]│
└──────────────────────────────────┘
```

**具体修改：**
- 删除顶部 gesture guide 区域（`gesture_guide_area`），简化为 touchpad + scroll strip + L/M/R
- 使用 `LinearLayout` 水平排列 touchpad（weight=5）和 scroll strip（weight=1）
- 下方水平排列 L/M/R 按键（weight 2:1:2）
- 外层使用 `FrameLayout` 包裹，touchpad 表面应用带纹理的背景 drawable
- 添加 Openterface 品牌 logo 在 touchpad 底部居中

**涉及文件：**
- `app/src/main/res/layout/module_portrait_mouse.xml` — 重写布局

#### Task 1.2：重写 `BasicPortraitScrollStripView` 为连续拖拽条

**参考 KeyCMD 的 `BasicPortraitScrollStripView`：**
- 改为自定义 `View`（继承 `View` 而非 `LinearLayout`），不再包含上/下两个按钮
- 手指上下拖拽时，通过 `STRIP_PIXELS_PER_WHEEL_UNIT` 比例累计滚轮值
- 每产生一个 wheel step 就发送一次 HID 滚轮事件并触发震动反馈
- 绘制装饰性的上/下 chevron 图标
- 支持灵敏度调节

**涉及文件：**
- `app/src/main/java/com/openterface/AOS/view/BasicPortraitScrollStripView.java` — 重写为连续拖拽 View

#### Task 1.3：重构 `MouseControlStripView` 为仅 L/M/R 按键行

**修改内容：**
- 删除 ScrollUp 和 ScrollDown 按钮（滚动功能已移至右侧 scroll strip）
- 仅保留 L / M / R 三个按键
- 按键样式改为 Material keycap 风格（白色/浅色底 + 边框 + 圆角）
- **添加震动反馈**：每次按键按下时调用 `performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)` 或 `TouchPadHaptics` 等效实现
- 按键按下时有视觉反馈（alpha 变化或颜色变化）

**涉及文件：**
- `app/src/main/java/com/openterface/AOS/view/MouseControlStripView.java` — 重构为 3 按键

#### Task 1.4：更新 `touchpad_overlay.xml` 横屏布局

**修改内容：**
- 同步竖屏的布局变更：touchpad + 右侧 scroll strip + 下方 L/M/R
- 删除横屏 gesture guide 区域
- 保持与竖屏一致的视觉风格

**涉及文件：**
- `app/src/main/res/layout/touchpad_overlay.xml` — 重写布局

#### Task 1.5：更新 MainActivity 中鼠标模块的绑定逻辑

**修改内容：**
- 更新 `MainActivity` 中引用 scroll strip 的代码，适配新的 `BasicPortraitScrollStripView` API
- 确保 scroll strip 的滚轮事件正确发送到 HID
- 确保 L/M/R 按键的震动反馈正确触发
- 更新 touchpad overlay 在横屏模式下的绑定

**涉及文件：**
- `app/src/main/java/com/openterface/AOS/activity/MainActivity.java` — 更新鼠标模块绑定

#### Task 1.6：编译测试

- 编译通过：`./gradlew assembleDebug`
- 功能验证：
  - 竖屏鼠标界面：scroll strip 连续拖拽滚动、L/M/R 有震动反馈
  - 横屏 touchpad overlay：布局正确、功能正常

---

### Phase 2：UI 配色和按键风格重构

#### Task 2.1：建立 Material Design 3 颜色系统

**新增/修改文件：**

1. **`app/src/main/res/values/colors.xml`** — 补充完整颜色 token：
   - 基础颜色：`background_light` (#F5F5F5)、`text_primary` (#212121)、`text_secondary` (#757575)
   - 表面颜色：`header_background` (#FFFFFF)、`card_background` (#FFFFFF)、`touchpad_background` (#E0E0E0)
   - 分隔线：`divider` (#BDBDBD)
   - 按键颜色：`key_bg_normal` (#FFFFFF)、`key_bg_function` (#E9E9EC)、`key_bg_pressed` (#FFE0B2)、`key_border` (#9A9AA0)、`key_text` (#1F1F24)
   - 主题色：`primary` (#F57C00，Openterface 橙色)
   - scroll strip chevron：`km_touchpad_scroll_strip_chevron` (#B0B0B0)

2. **`app/src/main/res/values-night/colors.xml`** — 新增深色模式颜色覆盖：
   - `background_light` → 深色 (#1C1C1E)
   - `text_primary` → 浅色 (#E0E0E0)
   - `header_background` → 深色 (#2C2C2E)
   - `key_bg_normal` → 深色 (#3A3A3C)
   - `key_text` → 浅色 (#E0E0E0)

#### Task 2.2：更新主题定义

**修改文件：**

1. **`app/src/main/res/values/themes.xml`** — 升级到 Material Design 3：
   - 将 `Theme.Usbvideo` 的 parent 改为 `Theme.Material3.DayNight.NoActionBar`
   - 设置 `colorPrimary`、`colorOnPrimary`、`colorSecondary` 等 Material 属性
   - 移除旧的紫色系配色
   - 添加 `ButtonTheme` 使用 Material keycap 背景

2. **`app/src/main/res/values-night/themes.xml`** — 新增深色主题覆盖

#### Task 2.3：创建按键和表面 Drawable 资源

**新增文件：**

1. **`app/src/main/res/drawable/key_background.xml`** — Material keycap 风格按键背景：
   - 白色（light）/ 深灰（dark）底色
   - 1dp 边框（`key_border` 色）
   - 8dp 圆角
   - pressed 状态变为 `key_bg_pressed` 色

2. **`app/src/main/res/drawable/km_basic_touchpad_surface.xml`** — Touchpad 表面纹理：
   - 多层 `layer-list`：底色 + 微光效果 + 阴影效果
   - 10dp 圆角
   - clipToOutline 支持

3. **`app/src/main/res/drawable/km_basic_touchpad_mouse_button_background.xml`** — 鼠标按键背景：
   - 与 keycap 风格一致但使用不同的按压色调

4. **`app/src/main/res/drawable/basic_compose_editor_background.xml`** — 编辑器表面：
   - 与 keycap 相同的纹理但无按压状态

#### Task 2.4：创建按键样式 Style

**修改文件：**

1. **`app/src/main/res/values/styles.xml`** — 新增 KeyCMD 兼容样式：
   - `Widget.KeyCmd.BasicTouchpadMouseButton` — L/M/R 鼠标按键
   - `Widget.KeyCmd.BasicNumpadCell` — 数字键盘按键
   - `KbMouseSubmodeTab` — 子模式切换标签
   - `Widget.KeyCmd.BasicComposeActionButton` — 操作按钮

#### Task 2.5：更新 `module_portrait_ime.xml` IME 模块样式

**修改内容：**
- 将所有硬编码颜色替换为 `@color/` 引用
- EditText 使用 `basic_compose_editor_background`
- 按钮使用 Material outlined button 风格
- Send 按钮使用主题色 `?attr/colorPrimary`
- 整体背景使用 `@color/background_light`

**涉及文件：**
- `app/src/main/res/layout/module_portrait_ime.xml` — 样式更新

#### Task 2.6：更新所有模块的背景和文字颜色

**修改内容：**
- `module_portrait_mouse.xml`：背景 `@color/background_light`，touchpad 使用 `km_basic_touchpad_surface`
- `touchpad_overlay.xml`：同步深色/浅色适配
- `MouseControlStripView.java`：按键使用 `key_background` drawable，颜色从 `@color/` 获取而非硬编码
- `BasicPortraitScrollStripView.java`：chevron 颜色使用 `@color/km_touchpad_scroll_strip_chevron`
- `PortraitKeyboardView.java`：背景使用 `@color/background_light`

**涉及文件：**
- 上述所有 view 类和布局文件

#### Task 2.7：创建 Chevron Drawable 资源

**新增文件：**

1. **`app/src/main/res/drawable/km_basic_scroll_strip_chevron_up.xml`** — 上箭头 chevron
2. **`app/src/main/res/drawable/km_basic_scroll_strip_chevron_down.xml`** — 下箭头 chevron

#### Task 2.8：编译测试

- 编译通过：`./gradlew assembleDebug`
- 视觉验证：
  - 浅色模式：按键为白色 keycap，背景为浅灰
  - 深色模式：按键为深灰，背景为深色
  - touchpad 表面有纹理效果
  - scroll strip chevron 颜色正确
- 功能验证：所有按键功能正常

---

### Phase 3：竖屏键盘布局重构

#### Task 3.1：重构 `PortraitKeyboardView` 支持完整键盘

**修改内容：**
- 移除旧的 `system_button` + `function_button` 面板切换模式
- 改为加载一个完整的竖屏键盘布局（参考 KeyCMD 的 `BasicPhysicalKeyboardView`）
- 键盘结构：
  - **Row 0**：Esc + F1~F12（横向滚动）
  - **Row 1**：数字行 (`1234567890`) + Backspace
  - **Row 2**：QWERTY 行 (`QWERTYUIOP`)
  - **Row 3**：ASDF 行 (`ASDFGHJKL`) + Enter
  - **Row 4**：ZXCV 行 (`ZXCVBNM`) + 符号切换
  - **Row 5**：修饰键行 (Ctrl / Alt / Win / Space / Alt / Ctrl)
  - **Row 6**：方向键行 (↑) + 底部功能 (←↓→)

**涉及文件：**
- `app/src/main/java/com/openterface/AOS/view/PortraitKeyboardView.java` — 重写
- `app/src/main/res/layout/module_portrait_keyboard.xml` — 更新

#### Task 3.2：实现动态按键创建与布局

**参考 KeyCMD 的 `BasicPhysicalKeyboardView`：**
- 通过 `inflateKey()` 方法动态创建按键 View
- 每个按键使用 `key_background` drawable
- 按键宽度按权重分配（普通键 weight=1，Shift/Enter weight=1.5，Space weight=5）
- 修饰键支持 tap（单次发送）和 long-press（latch 锁定）
- 支持 key preview popup（按下时浮动显示按键字符）

**涉及文件：**
- `app/src/main/java/com/openterface/AOS/view/PortraitKeyboardView.java` — 添加动态按键创建逻辑
- `app/src/main/res/layout/key_preview_popup.xml` — 确认已有，适配

#### Task 3.3：实现键盘功能层切换

**修改内容：**
- 添加键盘层切换功能：字母层 / 数字符号层 / 功能键层
- 底部工具栏：`[ABC]` `[123]` `[Fn]` 切换按钮
- `ABC`：QWERTY 字母键盘（默认）
- `123`：数字 + 常用符号键盘
- `Fn`：F1~F12 + 导航键（Home/End/PgUp/PgDn/Del/Ins）

**涉及文件：**
- `app/src/main/java/com/openterface/AOS/view/PortraitKeyboardView.java`

#### Task 3.4：实现修饰键 sticky latch

**参考 KeyCMD 的 `KmBasicHoldLockController`：**
- Ctrl / Shift / Alt / Win 支持 tap（单次发送修饰键 press+release）
- 长按修饰键（~1s）进入 latch 模式：按键高亮，修饰键持续按下
- 再次 tap 同一修饰键取消 latch
- latch 状态下按其他键时，修饰键随同发送

**涉及文件：**
- `app/src/main/java/com/openterface/AOS/view/PortraitKeyboardView.java` — 添加修饰键逻辑
- 可能需要新增 `ModifierKeyHelper` 辅助类

#### Task 3.5：更新键盘按键的 HID 发送

**修改内容：**
- 确保所有按键的 HID keycode 正确映射
- 修饰键使用 `HidManager` 的对应方法发送
- 字母键和数字键使用标准 HID keycode
- 功能键（F1~F12）正确映射

**涉及文件：**
- `app/src/main/java/com/openterface/AOS/view/PortraitKeyboardView.java`
- 可能需要更新 `KeyBoardManager` 或 `HidManager`

#### Task 3.6：编译测试

- 编译通过：`./gradlew assembleDebug`
- 功能验证：
  - 竖屏键盘显示完整 QWERTY 布局
  - 层切换（ABC / 123 / Fn）正常工作
  - 修饰键 latch 功能正常
  - 所有按键 HID 发送正确
  - 按键震动反馈正常
  - Key preview popup 正常显示

---

### Phase 4：集成测试与收尾

#### Task 4.1：端到端集成测试

- 竖屏模式：
  - 键盘模块 → 字母/数字/功能层切换 → HID 发送正确
  - 鼠标模块 → touchpad 手势 → scroll strip 拖拽 → L/M/R 按键 → HID 正确
  - IME 模块 → 文本输入 → 发送 → HID 正确
- 横屏模式：
  - touchpad overlay 正常工作
  - 布局与竖屏一致
- 主题切换：
  - 系统 light → dark 切换后所有 UI 颜色正确
  - 所有按键、背景、文字颜色适配

#### Task 4.2：震动反馈全面验证

- scroll strip 每个 wheel step 有轻微震动
- L/M/R 按键按下时有震动
- 键盘按键按下时有震动（如果之前没有）

#### Task 4.3：最终编译测试

- `./gradlew assembleDebug` 编译通过
- `./gradlew test` 单元测试通过
- 无 lint 警告（颜色硬编码、deprecated API 等）

---

## 三、关键文件清单

### 需要新建的文件
| 文件 | 说明 |
|------|------|
| `res/drawable/key_background.xml` | Material keycap 按键背景 selector |
| `res/drawable/km_basic_touchpad_surface.xml` | Touchpad 表面纹理 drawable |
| `res/drawable/km_basic_touchpad_mouse_button_background.xml` | 鼠标按键背景 |
| `res/drawable/km_basic_scroll_strip_chevron_up.xml` | 上 chevron 图标 |
| `res/drawable/km_basic_scroll_strip_chevron_down.xml` | 下 chevron 图标 |
| `res/drawable/basic_compose_editor_background.xml` | 编辑器表面 drawable |
| `res/values-night/colors.xml` | 深色模式颜色覆盖 |
| `res/values-night/themes.xml` | 深色模式主题覆盖 |
| `res/values/styles.xml` | 新增 KeyCMD 兼容样式（如尚不存在） |

### 需要修改的文件
| 文件 | 修改内容 |
|------|----------|
| `res/layout/module_portrait_mouse.xml` | 重写：touchpad + scroll strip + L/M/R |
| `res/layout/touchpad_overlay.xml` | 重写：横屏同步 |
| `res/layout/module_portrait_ime.xml` | 更新样式：使用主题颜色 |
| `res/layout/module_portrait_keyboard.xml` | 更新：适配新键盘 View |
| `res/values/colors.xml` | 补充完整颜色 token |
| `res/values/themes.xml` | 升级到 Material3 DayNight |
| `res/values/styles.xml` | 新增按键和组件样式 |
| `view/MouseControlStripView.java` | 重构为 L/M/R 3 按键 + 震动 |
| `view/BasicPortraitScrollStripView.java` | 重写为连续拖拽条 |
| `view/PortraitKeyboardView.java` | 重写：完整 QWERTY + 层切换 + 修饰键 |
| `activity/MainActivity.java` | 更新模块绑定逻辑 |

---

## 四、编译测试检查点

| 阶段 | 编译命令 | 验证项 |
|------|----------|--------|
| Phase 1 完成 | `./gradlew assembleDebug` | 鼠标布局正确、scroll strip 连续拖拽、L/M/R 震动 |
| Phase 2 完成 | `./gradlew assembleDebug` | Light/Dark 主题适配、按键 keycap 风格、无硬编码颜色 |
| Phase 3 完成 | `./gradlew assembleDebug` | QWERTY 键盘完整、层切换、修饰键 latch、HID 正确 |
| Phase 4 完成 | `./gradlew assembleDebug && ./gradlew test` | 全部功能正常、单元测试通过 |

---

## 五、注意事项

1. **每个 Phase 完成后必须编译测试**，确保不引入编译错误
2. **向后兼容**：不破坏横屏模式和已有的 USB/蓝牙连接功能
3. **性能**：自定义 View 的 `onDraw` 避免创建对象，避免过度绘制
4. **可维护性**：颜色和尺寸使用 `dimens.xml` / `colors.xml` 集中管理
5. **参考而非复制**：AOS 项目架构与 KeyCMD 不同（非 Fragment 子模式），适配时保持 AOS 的模块结构
