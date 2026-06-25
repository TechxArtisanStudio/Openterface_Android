# AOS Input (IME) 模块问题分析与改进计划

> 参考 KeyCMD Android 项目中 Keyboard & Mouse Pro 的 Compose 输入页面功能，对 AOS 项目的 Input 模块进行问题诊断和功能改进。

---

## 一、当前问题清单

### 问题 1: SEND 点击后没有正确发送到 Target

**症状**: 点击 SEND 按钮后，文本没有正确发送到目标设备。

**根因分析**:

1. **线程模型错误**: 当前 `sendTextViaHID()` 在 `runOnUiThread` 中执行 HID 发送 + `Thread.sleep(50)`，导致主线程阻塞
2. **Release 报文发送方式有缺陷**: 在 `runOnUiThread` 回调中嵌套 `Thread.sleep()` 并不可靠
3. **字符映射过于简化**: `charToScanCode()` 只覆盖了基础 ASCII 字符（a-z, A-Z, 0-9, 空格, 换行, `.`, `,`, `!`, `?`），大量常用字符（如 `-`, `_`, `@`, `#`, `$`, `%`, `^`, `&`, `*`, `(`, `)`, `{`, `}`, `[`, `]`, `;`, `:`, `'`, `"`, `\`, `|`, `/`, `<`, `>`, `~`, `` ` ``）都缺少映射
4. **Shift 键未正确处理**: 大写字母和符号需要 Shift 修饰键，但 `sendKeyBoardData(null, scanCode)` 第一个参数为 `null`（无修饰键），导致 Shift 组合键无法正确发送

**参考 KeyCMD 实现**:
- KeyCMD 使用 `HidTextKeystrokeSender` 在**后台线程**中发送 HID 数据
- 支持完整的字符到 HID 扫描码映射（通过 `km_hid_code_for_char` JNI 调用）
- 支持 `<CTRL>`, `<SHIFT>`, `<ALT>`, `<CMD>` 等修饰键标签
- 支持 `<DELAY1S>`, `<DELAY2S>` 等延时标签
- 使用 `ConnectionManager` 统一管理 USB/蓝牙连接

---

### 问题 2: 点击发送后所有文字被清除

**症状**: 发送完成后，EditText 中的文本被清空（`textInput.setText("")`），用户无法保留或再次发送。

**根因分析**:
```java
// MainActivity.java:3022
if (isSendingText) {
    Toast.makeText(MainActivity.this, "文本发送完成", Toast.LENGTH_SHORT).show();
    textInput.setText("");  // <-- 发送完成后无条件清空
    hideSendProgress();
}
```

**参考 KeyCMD 实现**:
- KeyCMD 的 `BasicComposeFragment` 通过 `clearEditorAfterSuccess` 参数控制是否清空
- 当 `isEmbeddedInKmPro()` 为 true（嵌入 KM Pro 模式）时，**发送后不清空编辑器**
- 仅在独立 Basic Compose 模式下才清空

**改进方案**: 发送完成后默认不清空编辑器，或提供选项让用户选择。

---

### 问题 3: 点击输入框时底部四个按钮消失

**症状**: 点击 EditText 获得焦点、弹出软键盘后，底部的 Clear / Save / Saved / Send 按钮被软键盘遮挡，无法看到和点击。

**根因分析**:
1. **布局问题**: 当前布局 `module_portrait_ime.xml` 中 EditText 使用 `layout_weight=1` 占满剩余空间，按钮行在 EditText 下方，当软键盘弹出时按钮被推出屏幕
2. **缺少 IME 键盘适配**: 没有处理 `WindowInsetsCompat.Type.ime()` 的底部内边距
3. **缺少 `SOFT_INPUT_ADJUST_RESIZE`**: 未正确设置软输入模式

**参考 KeyCMD 实现**:
- KeyCMD 的 `BasicComposeFragment` 通过 `setupBasicComposeImeInsets()` 处理 IME 窗口插入事件
- 使用 `Math.max(imeBottom, navigationBars.bottom)` 动态调整底部内边距
- 在横屏 IME 打开时隐藏操作行，竖屏时保持可见
- 设置 `Window.setSoftInputMode(SOFT_INPUT_ADJUST_RESIZE)`

---

### 问题 4: 保存文本功能过于简陋

**症状**: 当前保存功能只有一个文本字段，所有保存的文本拼接在一起，无法单独管理。

**根因分析**:
1. **存储方式**: 使用 `SharedPreferences("SavedTexts")` 存储单个字符串 `saved_text`，多条文本用换行符拼接
2. **无法区分条目**: 保存的文本无法单独查看、编辑、删除、重命名
3. **无置顶功能**: 没有 Pin/Favorite 功能
4. **无重命名功能**: 保存的文本无法重命名
5. **UI 过于简陋**: "已保存的文本"只是一个 AlertDialog 显示所有拼接文本 + "清空所有"按钮

**参考 KeyCMD 实现**:

| 功能 | KeyCMD 实现 | AOS 当前实现 |
|------|------------|-------------|
| 数据结构 | `SavedTextItem` (id, title, content, pinned, pinnedAt, createdAt, updatedAt) | 单个字符串，用换行符拼接 |
| 存储 | `SavedTextRepository` → Gson JSON → `SharedPreferences` | 单条 `saved_text` 字符串 |
| 条目管理 | 每条独立：可重命名、可删除、可置顶/取消置顶 | 只能"清空所有" |
| UI | BottomSheet 列表（RecyclerView），每条显示标题、时间、预览、📌图标、更多菜单 | AlertDialog 显示拼接文本 |
| 操作 | 选中条目后可：预览(Preview)、加载到编辑器(Load)、直接发送(Send) | 无 |
| 排序 | 置顶优先(pinnedAt 降序) → 更新时间降序 | 无排序 |
| 上限 | MAX_ITEMS=60 条，MAX_CONTENT_CHARS=24000, MAX_TITLE_CHARS=80 | 无限制（可能撑爆 SharedPreferences） |

---

### 问题 5: 缺少 Restore（恢复/撤销清除）功能

**症状**: 点击 Clear 后文本被永久清除，无法恢复。

**根因分析**:
```java
// MainActivity.java:2933-2936
clearButton.setOnClickListener(v -> {
    textInput.setText("");  // 直接清空，无快照
    Toast.makeText(this, "已清空文本", Toast.LENGTH_SHORT).show();
});
```

**参考 KeyCMD 实现**:
- KeyCMD 的 `BasicComposeFragment` 实现了 Clear / Undo Clear 切换：
  1. 第一次点击 Clear → 快照当前文本到 `undoSnapshot`，设置 `undoClearEligible = true`，清空编辑器
  2. Clear 按钮图标变为 **Undo（撤销）** 图标
  3. 第二次点击 → 调用 `performUndoClear()`，恢复快照文本
  4. 如果用户在清空后手动输入了新内容，`undoClearEligible` 自动重置为 false

---

### 问题 6: Send 进度显示不够好

**症状**: 发送时进度条显示但体验不佳。

**根因分析**:
1. 进度条使用原生 `ProgressBar` 而非 Material Design 的 `LinearProgressIndicator`
2. 没有根据文本长度智能决定是否显示进度（短文本不需要进度条）
3. 发送时 Send 按钮没有变为 Stop 按钮
4. 没有预估剩余时间的显示

**参考 KeyCMD 实现**:
- 使用 Material Design `LinearProgressIndicator`（`setIndeterminate(false)`, `setMax(1000)`）
- 阈值控制：只有 `totalUnits >= COMPOSE_SEND_PROGRESS_MIN_UNITS (56)` 时才显示进度条
- 节流更新：`COMPOSE_SEND_PROGRESS_UI_MIN_INTERVAL_MS = 67ms`
- 尾部不节流：最后 40 个单位时取消节流，确保最后一段平滑
- Send 按钮在发送中变为 Stop 图标（`ic_compose_stop_24`），点击可取消
- 进度标签显示剩余数量："剩余 N 个"

---

## 二、改进方案

### 改进 1: 修复 HID 发送 — 使用 `libkeymod.so` / `HidManager`

**目标**: 替换简化的 `charToScanCode()` 和阻塞式发送线程。

**方案**:

1. **启用 `HidManager` Core 实现**: 将 `HidManager.useCoreImplementation` 设为 `true`，使用 `KeyBoardManagerCore`（通过 `KeymodJNI` 调用 `libkeymod.so`）
2. **创建 `TextHidSender` 工具类**: 参考 KeyCMD 的 `HidTextKeystrokeSender`，实现完整的文本→HID 发送器
   - 支持完整 ASCII 字符映射（通过 `km_hid_code_for_char()`）
   - 支持修饰键标签 `<CTRL>`, `<SHIFT>`, `<ALT>`, `<CMD>`
   - 支持延时标签 `<DELAY1S>`, `<DELAY2S>`, `<DELAY5S>`, `<DELAY10S>`
   - 支持非 ASCII 字符高亮提示（参考 `NonAsciiTextHighlighter`）
3. **后台线程发送**: 使用专用线程 `"ime-send"` 发送 HID 数据，不阻塞 UI 线程
4. **进度回调**: 通过 `SendProgressListener` 回调进度更新到 UI 线程

**涉及文件**:
- 新建: `com/openterface/AOS/utils/TextHidSender.java`
- 修改: `MainActivity.java` — `sendTextViaHID()` 改为调用 `TextHidSender`

---

### 改进 2: 发送后保留文本

**方案**: 发送成功后不再自动清空编辑器，保留文本供用户再次编辑或发送。

**参考**: KeyCMD 的 `clearEditorAfterSuccess` 逻辑。

**涉及文件**:
- 修改: `MainActivity.java` — `sendTextViaHID()` 中移除 `textInput.setText("")`

---

### 改进 3: 修复软键盘遮挡按钮问题

**方案**:

1. **添加 IME 窗口插入监听**: 参考 KeyCMD 的 `setupBasicComposeImeInsets()`
   ```java
   ViewCompat.setOnApplyWindowInsetsListener(imeRoot, (v, windowInsets) -> {
       int imeBottom = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
       int navBottom = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
       int bottomInset = Math.max(imeBottom, navBottom);
       // 调整按钮行的底部内边距，确保不被软键盘遮挡
       imeRoot.setPadding(paddingStart, paddingTop, paddingEnd, paddingBottom + bottomInset);
       return windowInsets;
   });
   ```
2. **设置软输入模式**: `getWindow().setSoftInputMode(SOFT_INPUT_ADJUST_RESIZE)`
3. **按钮行始终可见**: 当软键盘弹出时，调整 EditText 高度使其缩小，按钮行保持在键盘上方

**涉及文件**:
- 修改: `MainActivity.java` — `setupImeModule()` 中添加窗口插入监听
- 修改: `module_portrait_ime.xml` — 确保布局层次支持动态内边距

---

### 改进 4: 重构保存文本管理系统

**方案**: 参考 KeyCMD 的 `SavedTextRepository` + `SavedTextItem` + `ImeSavedTextFragment`，实现完整的保存文本管理。

#### 4a: 数据层 — `SavedTextItem` + `SavedTextRepository`

1. **新建 `SavedTextItem.java`**:
   ```java
   public class SavedTextItem {
       public long id;           // 时间戳 ID
       public String title;      // 标题（可重命名）
       public String content;    // 内容
       public boolean pinned;    // 是否置顶
       public long pinnedAt;     // 置顶时间
       public long createdAt;    // 创建时间
       public long updatedAt;    // 更新时间
   }
   ```

2. **新建 `SavedTextRepository.java`**:
   - 使用 Gson 序列化 `List<SavedTextItem>` 到 `SharedPreferences("SavedTextPrefs")`
   - `loadSorted()`: 按置顶优先 → 更新时间降序排序
   - `addFromPlainText(content)`: 新增条目，自动提取标题（第一行非空文本）
   - `updateTitle(id, newTitle)`: 重命名
   - `setPinned(id, pinned)`: 置顶/取消置顶
   - `delete(id)`: 删除单条
   - 上限: MAX_ITEMS=60, MAX_CONTENT_CHARS=24000, MAX_TITLE_CHARS=80

#### 4b: UI 层 — BottomSheet 列表

1. **修改布局**: 用 BottomSheet 替代 AlertDialog
2. **每条显示**: 标题 + 相对时间 + 内容预览 + 📌图标 + 更多菜单（⋮）
3. **更多菜单**: Pin/Unpin、Rename、Delete
4. **选中操作**: 选中条目后显示 Preview / Load / Send 按钮
5. **保存按钮**: 将当前编辑器内容保存为新条目

**涉及文件**:
- 新建: `com/openterface/AOS/model/SavedTextItem.java`
- 新建: `com/openterface/AOS/utils/SavedTextRepository.java`
- 新建: `com/openterface/AOS/fragment/SavedTextsFragment.java`（或 BottomSheet）
- 新建: `res/layout/bottom_sheet_saved_texts.xml`
- 新建: `res/layout/item_saved_text_row.xml`
- 修改: `MainActivity.java` — `setupImeModule()` 中替换保存逻辑

---

### 改进 5: 添加 Restore（撤销清除）功能

**方案**: 参考 KeyCMD 的 Clear / Undo Clear 切换逻辑。

1. **添加字段**:
   ```java
   private String undoSnapshot;        // 清除前的文本快照
   private boolean undoClearEligible;  // 是否可以撤销清除
   ```

2. **Clear 按钮逻辑**:
   ```java
   clearButton.setOnClickListener(v -> {
       if (undoClearEligible && undoSnapshot != null && !undoSnapshot.isEmpty()) {
           // 第二次点击：恢复文本
           textInput.setText(undoSnapshot);
           textInput.setSelection(undoSnapshot.length());
           undoSnapshot = null;
           undoClearEligible = false;
           updateClearButtonIcon(); // 恢复为 Clear 图标
       } else {
           // 第一次点击：快照并清除
           undoSnapshot = textInput.getText().toString();
           undoClearEligible = true;
           textInput.setText("");
           updateClearButtonIcon(); // 变为 Undo 图标
       }
   });
   ```

3. **用户编辑时自动重置**: 如果用户在清除后输入了新内容，`undoClearEligible` 自动变为 false
4. **按钮图标切换**: Clear 图标 ↔ Undo 图标

**涉及文件**:
- 修改: `MainActivity.java` — `setupImeModule()` 中修改 Clear 按钮逻辑
- 修改: `module_portrait_ime.xml` — Clear 按钮改为图标按钮（或保持文本切换）

---

### 改进 6: 改进 Send 进度体验

**方案**: 参考 KeyCMD 的进度条和按钮切换逻辑。

1. **使用 Material Design LinearProgressIndicator**: 替换原生 ProgressBar
2. **阈值控制**: 文本长度 >= 56 字符时才显示进度条（短文本不需要）
3. **Send → Stop 切换**: 发送中 Send 按钮变为 Stop 按钮，点击可取消发送
4. **剩余数量标签**: 显示 "剩余 N 个"
5. **节流更新**: 67ms 最小间隔，最后 40 个单位不节流

**涉及文件**:
- 修改: `module_portrait_ime.xml` — ProgressBar → LinearProgressIndicator
- 修改: `MainActivity.java` — `sendTextViaHID()` 中改进进度逻辑

---

## 三、改进优先级

| 优先级 | 改进项 | 原因 |
|--------|--------|------|
| P0 (紧急) | 改进 1: 修复 HID 发送 | 核心功能完全不可用 |
| P0 (紧急) | 改进 3: 修复按钮被遮挡 | 核心 UI 不可用 |
| P1 (高) | 改进 5: 添加 Restore 功能 | 用户体验关键 |
| P1 (高) | 改进 2: 发送后保留文本 | 防止数据丢失 |
| P2 (中) | 改进 4: 重构保存文本管理 | 功能完善 |
| P2 (中) | 改进 6: 改进 Send 进度体验 | 体验提升 |

---

## 四、KeyCMD 参考文件索引

| 文件 | 用途 |
|------|------|
| `Openterface_KeyCmd_Android/app/src/main/java/com/openterface/fragment/BasicComposeFragment.java` | Compose 编辑器主 Fragment（Clear/Undo/Send/Save/Saved 完整逻辑） |
| `Openterface_KeyCmd_Android/app/src/main/java/com/openterface/keymod/compose/SavedTextItem.java` | 保存文本数据模型 |
| `Openterface_KeyCmd_Android/app/src/main/java/com/openterface/keymod/compose/SavedTextRepository.java` | 保存文本存储层（Gson + SharedPreferences） |
| `Openterface_KeyCmd_Android/app/src/main/java/com/openterface/keymod/compose/ImeSavedTextBottomSheet.java` | 保存文本 BottomSheet UI |
| `Openterface_KeyCmd_Android/app/src/main/java/com/openterface/fragment/ImeSavedTextFragment.java` | 保存文本全屏 Fragment |
| `Openterface_KeyCmd_Android/app/src/main/java/com/openterface/keymod/util/HidTextKeystrokeSender.java` | HID 文本发送器（完整字符映射 + 修饰键 + 延时） |
| `Openterface_KeyCmd_Android/app/src/main/res/layout/fragment_basic_compose.xml` | Compose 编辑器布局 |
| `Openterface_KeyCmd_Android/app/src/main/res/layout/bottom_sheet_ime_saved_text.xml` | 保存文本 BottomSheet 布局 |
| `Openterface_KeyCmd_Android/app/src/main/res/layout/item_ime_saved_text_row.xml` | 保存文本列表行布局 |
| `Openterface_KeyCmd_Android/app/src/main/res/menu/menu_ime_saved_text_row.xml` | 保存文本行菜单（Pin/Rename/Delete） |

---

## 五、AOS 需要修改的文件索引

| 文件 | 修改内容 |
|------|----------|
| `app/src/main/java/com/openterface/AOS/activity/MainActivity.java` | `setupImeModule()`, `sendTextViaHID()`, `charToScanCode()` 重写 |
| `app/src/main/res/layout/module_portrait_ime.xml` | 添加窗口适配、Restore 按钮、Material 进度条 |
| `app/src/main/java/com/openterface/AOS/model/SavedTextItem.java` | **新建** — 保存文本数据模型 |
| `app/src/main/java/com/openterface/AOS/utils/SavedTextRepository.java` | **新建** — 保存文本存储层 |
| `app/src/main/java/com/openterface/AOS/utils/TextHidSender.java` | **新建** — 文本 HID 发送器 |
| `app/src/main/java/com/openterface/AOS/fragment/SavedTextsFragment.java` | **新建** — 保存文本管理 Fragment |
| `app/src/main/res/layout/bottom_sheet_saved_texts.xml` | **新建** — 保存文本 BottomSheet 布局 |
| `app/src/main/res/layout/item_saved_text_row.xml` | **新建** — 保存文本列表行布局 |
| `app/src/main/res/menu/menu_saved_text_row.xml` | **新建** — 保存文本行菜单 |
