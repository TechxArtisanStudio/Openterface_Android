# Openterface Android 键盘鼠标Pro功能修复计划

## 📋 项目概述

**目标**: 参考 Openterface_KeyCmd_Android 项目的 Keyboard Mouse Pro 页面实现,优化 Openterface_Android 项目的键盘、鼠标、输入(IME)和设置功能。

**当前状态**: 已分析完 KeyCMD 项目的核心实现,需要对照修复 Openterface_Android 项目中的相关问题。

**参考项目**: `/Users/txa/project/Openterface_KeyCmd_Android`

---

## 🎯 修复范围

### 1. 键盘功能 (Keyboard)
### 2. 鼠标功能 (Mouse/TouchPad)
### 3. 输入功能 (IME/Compose)
### 4. 设置功能 (Settings)

---

## 📊 任务分解与时间表

### 阶段一: 键盘功能优化 (预计 3-4 天)

#### 1.1 键盘布局重构 (Day 1-2)

**目标**: 实现与 KeyCMD 一致的键盘布局,包括按键大小、间距、功能键位置

**参考文件**:
- `KeyCMD: app/src/main/res/layout/fragment_keyboard.xml`
- `KeyCMD: app/src/main/res/layout/fragment_basic_keyboard.xml`
- `KeyCMD: app/src/main/java/com/openterface/keymod/basic/BasicPhysicalKeyboardView.java`

**任务清单**:
- [ ] 分析 KeyCMD 键盘布局结构(按键矩阵、功能键排列)
- [ ] 对比 Openterface_Android 当前键盘布局
- [ ] 重构按键布局 XML(调整按键大小、间距、对齐)
- [ ] 实现按键按下视觉反馈(气泡提示)
- [ ] 优化修饰键(Ctrl/Alt/Shift/Win)的长按锁定逻辑
- [ ] 测试键盘布局在不同屏幕尺寸下的适配

**验收标准**:
- ✅ 键盘布局与 KeyCMD 保持一致
- ✅ 按键按下时有气泡提示
- ✅ 修饰键长按可锁定状态
- ✅ 横屏和竖屏模式都能正常显示

#### 1.2 快捷键条优化 (Day 2)

**目标**: 实现顶部的快捷键条(Ctrl+C/V/A/Z/S 等)

**参考文件**:
- `KeyCMD: app/src/main/res/layout/include_pro_touchpad_mouse_keys.xml`
- `KeyCMD: app/src/main/res/layout-port/include_pro_touchpad_mouse_keys.xml`

**任务清单**:
- [ ] 分析 KeyCMD 快捷键条的布局和功能
- [ ] 在 Openterface_Android 中实现快捷键条
- [ ] 实现快捷键的点击事件处理
- [ ] 测试快捷键功能是否正常

**验收标准**:
- ✅ 快捷键条显示在键盘顶部
- ✅ 点击快捷键可正常发送对应的组合键
- ✅ 横屏和竖屏模式都能正常显示

#### 1.3 键盘设置功能 (Day 3)

**目标**: 实现键盘的音效、振动反馈设置

**参考文件**:
- `KeyCMD: app/src/main/java/com/openterface/keymod/fragments/KeyboardMouseSettingsFragment.java`

**任务清单**:
- [ ] 分析 KeyCMD 的键盘设置界面
- [ ] 在 Openterface_Android 设置页面添加键盘设置选项
- [ ] 实现按键音效开关和音量调节
- [ ] 实现按键振动反馈开关和强度调节
- [ ] 保存用户设置到 SharedPreferences

**验收标准**:
- ✅ 可以开关按键音效
- ✅ 可以调节音效音量
- ✅ 可以开关振动反馈
- ✅ 可以调节振动强度
- ✅ 设置保存后重启应用仍然有效

#### 1.4 键盘功能测试与Bug修复 (Day 4)

**任务清单**:
- [ ] 测试所有按键是否正常响应
- [ ] 测试组合键(Ctrl+C, Ctrl+V等)是否正常
- [ ] 测试修饰键锁定/解锁逻辑
- [ ] 测试快捷键条功能
- [ ] 修复发现的问题

**验收标准**:
- ✅ 所有按键功能正常
- ✅ 无崩溃、无卡顿
- ✅ 用户体验流畅

---

### 阶段二: 鼠标功能优化 (预计 2-3 天)

#### 2.1 触摸板布局优化 (Day 5)

**目标**: 简化布局,移除触摸区域,只保留 L/M/R 按键和滚动条,并在按键上方添加鼠标手势使用说明

**参考文件**:
- `KeyCMD: app/src/main/res/layout/fragment_mouse.xml`
- `KeyCMD: app/src/main/res/layout-port/fragment_touchpad.xml`

**任务清单**:
- [ ] 移除触摸板触摸区域(不需要 TouchPadView)
- [ ] 实现 L(左键)、M(中键)、R(右键) 按钮布局
- [ ] 实现滚轮功能按钮(上滚/下滚)
- [ ] 在 L/M/R 按键上方添加鼠标手势使用说明区域
- [ ] 为每个手势添加图标说明:
  - [ ] 左键点击/长按拖动
  - [ ] 右键单击
  - [ ] 中键/滚轮点击
  - [ ] 滚轮上滚/下滚
- [ ] 优化说明区域的布局和美观度

**验收标准**:
- ✅ 无触摸板区域,界面简洁
- ✅ L/M/R 按钮正常显示和响应
- ✅ 滚轮功能正常
- ✅ 鼠标手势使用说明在按键上方清晰显示
- ✅ 每个手势都有对应的图标辅助说明
- ✅ 横屏和竖屏模式都能正常显示

#### 2.2 按键图标资源准备 (Day 6)

**目标**: 设计和准备 L/M/R 按键及滚动功能的图标资源

**任务清单**:
- [ ] 设计左键单击图标(L按键说明用)
- [ ] 设计中键/滚轮图标(M按键说明用)
- [ ] 设计右键单击图标(R按键说明用)
- [ ] 设计滚轮上滚图标
- [ ] 设计滚轮下滚图标
- [ ] 准备不同分辨率的图标资源(mdpi, hdpi, xhdpi, xxhdpi)
- [ ] 编写使用说明文案(简洁明了)

**验收标准**:
- ✅ 所有图标清晰美观,符合 Material Design 规范
- ✅ 图标在不同分辨率下显示正常
- ✅ 使用说明文案简洁易懂

#### 2.3 鼠标按钮和滚动功能实现 (Day 7)

**目标**: 实现 L/M/R 按钮和滚轮的完整功能

**任务清单**:
- [ ] 实现左键(L)点击事件处理
- [ ] 实现中键(M)点击事件处理
- [ ] 实现右键(R)点击事件处理
- [ ] 实现左键长按拖动功能
- [ ] 实现滚轮上滚按钮事件处理
- [ ] 实现滚轮下滚按钮事件处理
- [ ] 实现滚轮连续滚动(长按加速)
- [ ] 实现按钮按下/释放状态管理
- [ ] 添加按钮按下的视觉反馈
- [ ] 测试所有按钮和滚动功能

**验收标准**:
- ✅ 所有按钮都能正确发送对应的鼠标事件
- ✅ 左键长按可实现拖动
- ✅ 滚轮可单次滚动和连续滚动
- ✅ 按钮状态(按下/释放)正确管理
- ✅ 按钮有清晰的视觉反馈
- ✅ 无误触、无延迟

#### 2.4 鼠标功能测试与Bug修复 (Day 7)

**任务清单**:
- [ ] 测试 L/M/R 按钮的点击功能
- [ ] 测试左键长按拖动功能
- [ ] 测试滚轮上下滚动功能
- [ ] 测试滚轮长按连续滚动
- [ ] 测试使用说明的显示效果
- [ ] 测试图标在不同设备上的显示
- [ ] 修复发现的问题

**验收标准**:
- ✅ 所有按钮功能正常
- ✅ 拖动功能流畅
- ✅ 滚动功能响应灵敏
- ✅ 使用说明清晰易懂
- ✅ 无崩溃、无卡顿
- ✅ 用户体验流畅

---

### 阶段三: 输入(IME)功能优化 (预计 2-3 天)

#### 3.1 输入界面重构 (Day 8)

**目标**: 实现与 KeyCMD 一致的输入界面,包括文本框、清除、撤销、保存文本、已保存文本、发送按钮

**参考文件**:
- `KeyCMD: app/src/main/res/layout/fragment_basic_compose.xml`
- `KeyCMD: app/src/main/res/layout-port/fragment_compose.xml`
- `KeyCMD: app/src/main/java/com/openterface/fragment/BasicComposeFragment.java`

**任务清单**:
- [ ] 分析 KeyCMD 输入界面布局
- [ ] 重构 Openterface_Android 的输入界面
- [ ] 实现多行文本输入框
- [ ] 实现清除按钮(带撤销功能)
- [ ] 实现保存文本按钮
- [ ] 实现已保存文本按钮(打开已保存文本列表)
- [ ] 实现发送按钮(改为 "Send" 而不是 "SEND AS KEYSTROKES")
- [ ] 移除 Ctrl/Alt/Shift 等修饰键按钮

**验收标准**:
- ✅ 输入界面与 KeyCMD 保持一致
- ✅ 所有按钮功能正常
- ✅ 横屏和竖屏模式都能正常显示

#### 3.2 文本发送逻辑优化 (Day 9)

**目标**: 实现与 KeyCMD 一致的文本发送逻辑,包括发送前验证、发送进度显示

**参考文件**:
- `KeyCMD: app/src/main/java/com/openterface/fragment/BasicComposeFragment.java`
- `KeyCMD: app/src/main/java/com/openterface/keymod/util/HidTextKeystrokeSender.java`

**任务清单**:
- [ ] 分析 KeyCMD 的文本发送逻辑
- [ ] 实现发送前的文本验证(检查是否支持所有字符)
- [ ] 实现发送进度条显示
- [ ] 实现发送取消功能(发送过程中可以停止)
- [ ] 实现发送完成后的清空文本框
- [ ] 优化发送性能(字符间延迟控制)

**验收标准**:
- ✅ 发送前能正确验证文本
- ✅ 发送进度条正常显示
- ✅ 可以取消发送
- ✅ 发送完成后文本框清空

#### 3.3 已保存文本功能 (Day 10)

**目标**: 实现已保存文本的存储、查看、编辑、删除功能

**参考文件**:
- `KeyCMD: app/src/main/java/com/openterface/keymod/compose/SavedTextRepository.java`
- `KeyCMD: app/src/main/java/com/openterface/fragment/ImeSavedTextFragment.java`
- `KeyCMD: app/src/main/res/layout/fragment_ime_saved_text.xml`

**任务清单**:
- [ ] 实现已保存文本的数据模型(SavedTextItem)
- [ ] 实现已保存文本的存储功能(SavedTextRepository)
- [ ] 实现已保存文本列表界面
- [ ] 实现添加新文本功能
- [ ] 实现编辑已有文本功能
- [ ] 实现删除文本功能
- [ ] 实现点击文本插入到输入框功能

**验收标准**:
- ✅ 可以保存文本
- ✅ 可以查看已保存文本列表
- ✅ 可以编辑已保存文本
- ✅ 可以删除已保存文本
- ✅ 点击文本可以插入到输入框

#### 3.4 输入功能测试与Bug修复 (Day 10)

**任务清单**:
- [ ] 测试文本输入功能
- [ ] 测试文本发送功能
- [ ] 测试已保存文本功能
- [ ] 修复发现的问题

**验收标准**:
- ✅ 所有功能正常
- ✅ 无崩溃、无卡顿
- ✅ 用户体验流畅

---

### 阶段四: 设置功能优化 (预计 1-2 天)

#### 4.1 设置界面重构 (Day 11)

**目标**: 实现与 KeyCMD 一致的设置界面,包括键盘、鼠标、输入的所有设置项

**参考文件**:
- `KeyCMD: app/src/main/java/com/openterface/keymod/fragments/KeyboardMouseSettingsFragment.java`
- `KeyCMD: app/src/main/res/layout/fragment_settings_keyboard_mouse.xml`

**任务清单**:
- [ ] 分析 KeyCMD 设置界面布局
- [ ] 重构 Openterface_Android 的设置界面
- [ ] 添加键盘设置分区
- [ ] 添加鼠标设置分区
- [ ] 添加输入设置分区
- [ ] 优化设置界面的美观度

**验收标准**:
- ✅ 设置界面布局清晰、美观
- ✅ 所有设置项都能正常显示
- ✅ 设置项分组合理

#### 4.2 设置功能实现 (Day 12)

**目标**: 实现所有设置项的功能,包括开关、滑动条、下拉选择等

**任务清单**:
- [ ] 实现键盘设置功能
  - [ ] 按键音效开关
  - [ ] 音效音量调节
  - [ ] 振动反馈开关
  - [ ] 振动强度调节
- [ ] 实现鼠标设置功能
  - [ ] 触摸灵敏度调节
  - [ ] 鼠标速度调节
  - [ ] 滚轮速度调节
- [ ] 实现输入设置功能
  - [ ] 自动保存草稿开关
  - [ ] 发送前验证开关
- [ ] 实现设置的持久化存储

**验收标准**:
- ✅ 所有设置项功能正常
- ✅ 设置保存后重启应用仍然有效
- ✅ 设置项之间有合理的联动关系

#### 4.3 设置功能测试与Bug修复 (Day 12)

**任务清单**:
- [ ] 测试所有设置项功能
- [ ] 测试设置保存和加载
- [ ] 修复发现的问题

**验收标准**:
- ✅ 所有功能正常
- ✅ 无崩溃、无卡顿
- ✅ 用户体验流畅

---

### 阶段五: 综合测试与优化 (预计 2 天)

#### 5.1 横屏/竖屏模式测试 (Day 13)

**任务清单**:
- [ ] 测试横屏模式下的键盘、鼠标、输入、设置功能
- [ ] 测试竖屏模式下的键盘、鼠标、输入、设置功能
- [ ] 测试横竖屏切换时的状态保持
- [ ] 修复发现的问题

**验收标准**:
- ✅ 横屏模式所有功能正常
- ✅ 竖屏模式所有功能正常
- ✅ 横竖屏切换时状态保持正确

#### 5.2 性能优化 (Day 14)

**任务清单**:
- [ ] 优化键盘响应速度
- [ ] 优化鼠标响应速度
- [ ] 优化文本发送性能
- [ ] 优化内存占用
- [ ] 减少 GC 频率

**验收标准**:
- ✅ 键盘响应延迟 < 50ms
- ✅ 鼠标响应延迟 < 50ms
- ✅ 文本发送速度流畅
- ✅ 内存占用稳定
- ✅ 无明显卡顿

#### 5.3 用户体验优化 (Day 14)

**任务清单**:
- [ ] 优化界面动画效果
- [ ] 优化按键反馈效果
- [ ] 优化触摸反馈效果
- [ ] 优化错误提示
- [ ] 添加必要的帮助文档

**验收标准**:
- ✅ 界面动画流畅自然
- ✅ 按键反馈清晰明确
- ✅ 触摸反馈灵敏准确
- ✅ 错误提示友好易懂
- ✅ 帮助文档完整清晰

#### 5.4 最终测试 (Day 14)

**任务清单**:
- [ ] 完整功能测试
- [ ] 边界情况测试
- [ ] 长时间运行稳定性测试
- [ ] 不同设备兼容性测试
- [ ] 修复最后发现的问题

**验收标准**:
- ✅ 所有功能正常
- ✅ 无崩溃、无卡顿
- ✅ 长时间运行稳定
- ✅ 多设备兼容良好

---

## 🔧 关键技术点

### 1. 按键按下气泡提示

**参考实现**:
```xml
<!-- KeyCMD: app/src/main/res/layout/basic_key_preview_popup.xml -->
<TextView
    android:id="@+id/key_preview_text"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:background="@drawable/key_preview_background"
    android:textColor="@android:color/white"
    android:textSize="24sp"
    android:padding="16dp" />
```

**实现思路**:
- 创建 `KeyPreviewPopup` 类管理气泡提示
- 在按键按下时显示气泡,抬起时隐藏
- 气泡位置跟随手指位置

### 2. 修饰键长按锁定

**参考实现**:
```java
// KeyCMD: KmBasicHoldLockController
public class KmBasicHoldLockController {
    private boolean ctrlLocked = false;
    private boolean altLocked = false;
    private boolean shiftLocked = false;
    
    public void onModifierLongPress(int modifier) {
        // 切换锁定状态
        toggleLock(modifier);
    }
}
```

**实现思路**:
- 使用 `OnLongClickListener` 检测长按
- 长按时切换锁定状态
- 锁定状态用不同颜色标识
- 按下时激活修饰键,释放时如果是锁定状态则保持激活

### 3. 鼠标按键和滚动实现

**参考实现**:
```java
// 鼠标按钮点击
public class MouseButton extends AppCompatButton {
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 发送鼠标按下事件
                sendMouseEvent(MOUSE_BUTTON_DOWN);
                // 视觉反馈
                setPressed(true);
                break;
            case MotionEvent.ACTION_UP:
                // 发送鼠标释放事件
                sendMouseEvent(MOUSE_BUTTON_UP);
                setPressed(false);
                break;
        }
        return true;
    }
}

// 滚轮连续滚动
private Handler scrollHandler = new Handler();
private Runnable scrollRunnable = new Runnable() {
    @Override
    public void run() {
        // 发送滚轮事件
        sendScrollEvent(scrollDirection);
        // 加速滚动
        scrollDelay = Math.max(MIN_DELAY, scrollDelay - ACCELERATION);
        scrollHandler.postDelayed(this, scrollDelay);
    }
};
```

**实现思路**:
- L/M/R 按钮使用自定义 Button 处理触摸事件
- 按下时发送 MOUSE_BUTTON_DOWN,抬起时发送 MOUSE_BUTTON_UP
- 左键长按超过阈值时进入拖动模式
- 滚轮按钮长按时使用 Handler 实现连续滚动
- 长按时间越长,滚动速度越快(加速效果)

### 4. 文本发送逻辑

**参考实现**:
```java
// KeyCMD: HidTextKeystrokeSender
public void sendText(String text, ProgressCallback callback) {
    for (int i = 0; i < text.length(); i++) {
        char c = text.charAt(i);
        sendCharacter(c);
        // 更新进度
        callback.onProgress(i, text.length());
        // 控制发送速度
        Thread.sleep(CHAR_DELAY_MS);
    }
}
```

**实现思路**:
- 逐字符发送 HID 键盘事件
- 使用 `Thread` 或 `Handler` 控制发送速度
- 通过回调更新进度条
- 支持取消操作

---

## 📝 每日工作计划

### Week 1

**Day 1 (周一)**
- [ ] 分析 KeyCMD 键盘布局结构
- [ ] 对比 Openterface_Android 当前键盘布局
- [ ] 开始重构按键布局 XML

**Day 2 (周二)**
- [ ] 完成键盘布局重构
- [ ] 实现按键按下视觉反馈(气泡提示)
- [ ] 实现快捷键条

**Day 3 (周三)**
- [ ] 实现修饰键长按锁定逻辑
- [ ] 实现键盘设置功能
- [ ] 测试键盘功能

**Day 4 (周四)**
- [ ] 实现鼠标界面布局(L/M/R 按键 + 滚动条)
- [ ] 设计和添加功能图标
- [ ] 实现使用说明显示

**Day 5 (周五)**
- [ ] 实现 L/M/R 按键功能(点击、长按拖动)
- [ ] 实现滚轮功能(单次、连续滚动)
- [ ] 测试鼠标功能

### Week 2

**Day 6 (周一)**
- [ ] 重构输入界面布局
- [ ] 实现清除/撤销功能
- [ ] 实现保存文本功能

**Day 7 (周二)**
- [ ] 实现已保存文本列表
- [ ] 实现文本发送逻辑
- [ ] 实现发送进度条

**Day 8 (周三)**
- [ ] 实现已保存文本的编辑/删除功能
- [ ] 测试输入功能

**Day 9 (周四)**
- [ ] 重构设置界面
- [ ] 实现所有设置项功能
- [ ] 测试设置功能

**Day 10 (周五)**
- [ ] 横屏/竖屏模式测试
- [ ] 性能优化
- [ ] 用户体验优化

---

## ✅ 验收标准总结

### 功能验收
- ✅ 键盘布局与 KeyCMD 保持一致
- ✅ 按键按下有气泡提示
- ✅ 修饰键长按可锁定
- ✅ 鼠标界面简洁,只保留 L/M/R 按键和滚动条
- ✅ 使用说明清晰显示在按键上方
- ✅ 每个功能都有对应的图标说明
- ✅ L/M/R 按钮和滚轮功能正常
- ✅ 左键长按可拖动
- ✅ 滚轮长按可连续滚动
- ✅ 输入界面与 KeyCMD 保持一致
- ✅ 文本发送有进度显示
- ✅ 已保存文本功能完整
- ✅ 设置界面功能完整

### 性能验收
- ✅ 键盘响应延迟 < 50ms
- ✅ 鼠标响应延迟 < 50ms
- ✅ 文本发送流畅无卡顿
- ✅ 内存占用稳定
- ✅ 长时间运行稳定

### 兼容性验收
- ✅ 横屏模式功能正常
- ✅ 竖屏模式功能正常
- ✅ 横竖屏切换状态保持正确
- ✅ 多设备兼容良好

---

## 🚀 风险与应对

### 风险1: 键盘布局适配困难
**影响**: 可能导致按键大小不合适或布局混乱
**应对**: 
- 提前在不同屏幕尺寸的设备上测试
- 使用 ConstraintLayout 实现自适应布局
- 预留调整时间

### 风险2: 按键和滚动响应不灵敏
**影响**: 可能导致用户体验差,点击无响应或延迟
**应对**:
- 充分测试各种点击和长按场景
- 优化事件处理逻辑,减少延迟
- 调整长按阈值和滚动速度参数
- 添加清晰的视觉反馈

### 风险3: 文本发送性能问题
**影响**: 可能导致发送卡顿或失败
**应对**:
- 优化字符间延迟
- 使用异步发送
- 添加发送队列机制

### 风险4: 横竖屏切换状态丢失
**影响**: 可能导致用户操作中断
**应对**:
- 使用 `onSaveInstanceState` 保存状态
- 在 `onConfigurationChanged` 中恢复状态
- 测试各种切换场景

---

## 📚 参考资源

### KeyCMD 项目核心文件
- `app/src/main/res/layout/fragment_keyboard.xml` - 键盘布局
- `app/src/main/res/layout/fragment_mouse.xml` - 鼠标布局
- `app/src/main/res/layout/fragment_basic_compose.xml` - 输入布局
- `app/src/main/res/layout-port/` - 竖屏布局
- `app/src/main/java/com/openterface/keymod/basic/BasicPhysicalKeyboardView.java` - 键盘视图
- `app/src/main/java/com/openterface/fragment/BasicComposeFragment.java` - 输入逻辑
- `app/src/main/java/com/openterface/keymod/fragments/KeyboardMouseSettingsFragment.java` - 设置逻辑

### Android 开发文档
- [Keyboard Input](https://developer.android.com/training/keyboard-input)
- [Touch Gestures](https://developer.android.com/training/gestures)
- [SharedPreferences](https://developer.android.com/training/data-storage/shared-preferences)

---

## 📞 联系与支持

如有问题,请参考 KeyCMD 项目的实现或查阅 Android 开发文档。

---

**文档版本**: v1.0  
**创建时间**: 2026-03-19  
**最后更新**: 2026-03-19  
**维护者**: AI Assistant
