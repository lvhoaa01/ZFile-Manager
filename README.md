# ZFile Manager


> Ứng dụng **quản lý file trên Android**, viết bằng **Java thuần**, theo kiến trúc **MVVM 5 tầng**. Hỗ trợ duyệt thư mục, sao chép/di chuyển, thùng rác (xoá mềm), nén/giải nén ZIP, tìm kiếm đệ quy, xem ảnh/video/nhạc/text ngay trong app, phân tích dung lượng bộ nhớ và đổi giao diện sáng/tối.

> Link video : https://drive.google.com/drive/folders/1Jb_fXh8xkjwvSY_2OFjR6GsOG4-y3mrX?usp=drive_link

---

## Mục lục

- [Tính năng](#tính-năng)
- [Kiến trúc tổng quan](#kiến-trúc-tổng-quan)
- [Công nghệ & thư viện](#công-nghệ--thư-viện)
- [Trình bày chi tiết các module](#trình-bày-chi-tiết-các-module)
  - [Tầng UI](#1-tầng-ui--comzfilemanagerui)
  - [Tầng ViewModel](#2-tầng-viewmodel--comzfilemanagerviewmodel)
  - [Tầng Repository](#3-tầng-repository--comzfilemanagerrepository)
  - [Tầng Service](#4-tầng-service--comzfilemanagerservice)
  - [Tầng Core (Singletons hạ tầng)](#5-tầng-core--comzfilemanagercore)
  - [Tầng Model](#6-tầng-model--comzfilemanagermodel)
  - [Tầng Util](#7-tầng-util--comzfilemanagerutil)
- [Các luồng chức năng tiêu biểu](#các-luồng-chức-năng-tiêu-biểu)
- [Mẫu thiết kế (Design Patterns)](#mẫu-thiết-kế-design-patterns)
- [Mô hình đa luồng](#mô-hình-đa-luồng)
- [Quyền truy cập](#quyền-truy-cập)
- [Cấu trúc thư mục](#cấu-trúc-thư-mục)
- [Build & chạy](#build--chạy)

---

## Tính năng

| Nhóm | Chức năng |
|---|---|
| **Duyệt file** | Duyệt thư mục theo ổ đĩa, breadcrumb (đường dẫn phân cấp bấm được), hiện/ẩn file ẩn, sắp xếp theo tên/kích thước/ngày/loại, ưu tiên thư mục lên trước |
| **CRUD** | Tạo file, tạo thư mục, đổi tên, xoá; sao chép / di chuyển (copy / cut / paste) có thanh tiến độ và huỷ giữa chừng |
| **Thùng rác** | Xoá mềm vào thùng rác, khôi phục về vị trí cũ, xoá vĩnh viễn, tự động dọn theo lịch (cấu hình số ngày giữ) |
| **Nén / Giải nén** | Nén nhiều file đã chọn thành `.zip`, giải nén `.zip` tại chỗ |
| **Tìm kiếm** | Tìm theo tên đệ quy từ thư mục hiện tại, có debounce và trả kết quả theo lô |
| **Phân loại** | Liệt kê file theo nhóm (Ảnh / Video / Nhạc / Tài liệu / Tải về / APK) dựa trên MediaStore |
| **Phân tích bộ nhớ** | Biểu đồ tròn dung lượng theo loại, top file & top thư mục lớn nhất |
| **Trình xem trong app** | Ảnh (zoom/pan/swipe), Text/code (tô màu cú pháp), Video, Nhạc |
| **Dấu trang** | Đánh dấu thư mục yêu thích để truy cập nhanh |
| **Cài đặt** | Giao diện Sáng / Tối / Theo hệ thống, Material You (Dynamic Color), số ngày giữ thùng rác |

---


## Kiến trúc tổng quan

Ứng dụng theo mô hình **MVVM 5 tầng**. Quy tắc vàng: **mỗi tầng chỉ gọi xuống tầng ngay dưới nó**, dữ liệu **đi xuống bằng lời gọi hàm** và **đi lên bằng LiveData**.

```
┌──────────────────────────────────────────────────────────────┐
│    UI          Fragment / Activity · Adapter · Custom View   │
│                  · Dialog Builder                              │
│                  (chỉ hiển thị & nhận thao tác, không I/O)     │
└───────────────┬──────────────────────────────────────────────┘
                │  gọi hàm ↓            LiveData (observe) ↑
┌───────────────┴──────────────────────────────────────────────┐
│    VIEWMODEL   Giữ trạng thái màn hình bằng LiveData,        │
│                  điều phối tác vụ xuống luồng nền              │
└───────────────┬──────────────────────────────────────────────┘
                │
┌───────────────┴──────────────────────────────────────────────┐
│    REPOSITORY  Facade: che giấu service, gắn Decorator,      │
│                  hợp nhất dữ liệu từ nhiều nguồn               │
└───────────────┬──────────────────────────────────────────────┘
                │
┌───────────────┴──────────────────────────────────────────────┐
│    SERVICE     POJO thuần làm I/O thật (đồng bộ/blocking):   │
│                  scan, copy, search, zip, trash, analyze       │
└───────────────┬──────────────────────────────────────────────┘
                │
┌───────────────┴──────────────────────────────────────────────┐
│    DATA        java.io.File · MediaStore · SharedPreferences │
│                  · java.util.zip · StatFs                      │
└──────────────────────────────────────────────────────────────┘

        🔧 CORE Singletons (dùng chung mọi tầng):
        ThreadPoolManager · FileSystemManager · AppSettings
        · TrashIndex · BookmarkStore · MimeTypeHelper
```


**Nguyên tắc nền tảng:**

- **UI không bao giờ tự đọc/ghi file.** Mọi I/O đẩy xuống luồng nền qua `ThreadPoolManager`, kết quả trả về bằng `LiveData.postValue`.
- **Service là POJO thuần** — không phụ thuộc Android UI, không tự tạo luồng (người gọi chịu trách nhiệm lập lịch). Nhờ vậy dễ kiểm thử bằng JUnit.
- **Huỷ tác vụ bằng cờ riêng cho từng tác vụ** (`AtomicBoolean`), không huỷ chéo.
- **Không dùng thư viện DI** (Hilt/Dagger): các phụ thuộc dùng chung được quản lý bằng Singleton khởi tạo trong `ZFileApplication`.

---

## Công nghệ & thư viện

| Hạng mục | Lựa chọn |
|---|---|
| Ngôn ngữ | Java 17 |
| compileSdk / targetSdk / minSdk | 35 / 35 / 24 |
| Kiến trúc | MVVM + Repository, View Binding |
| Lifecycle | `lifecycle-viewmodel`, `lifecycle-livedata`, `lifecycle-runtime` 2.8.7 |
| Điều hướng | `androidx.navigation` 2.8.5 (single-activity) |
| Giao diện | Material Components 1.12.0 (Material 3 + Dynamic Color), ConstraintLayout, RecyclerView, SwipeRefreshLayout, ViewPager2 |
| Tải ảnh | Glide 4.16.0 (thumbnail + xem ảnh) |
| Splash | `core-splashscreen` 1.0.1 |
| Truy cập file | `documentfile` 1.0.1, `FileProvider` (chia sẻ file ra app ngoài) |
| Lưu trữ | SharedPreferences + `org.json` (không dùng Room/SQLite) |
| Kiểm thử | JUnit 4, Espresso |

---

## Trình bày chi tiết các module

Mã nguồn nằm dưới `app/src/main/java/com/zfile/manager/`, chia theo tầng kiến trúc.

### Điểm khởi động

| Lớp | Vai trò |
|---|---|
| [`ZFileApplication`](app/src/main/java/com/zfile/manager/ZFileApplication.java) | Điểm vào tiến trình. Khởi tạo **theo đúng thứ tự**: nạp `AppSettings` → áp theme đã lưu → khởi tạo `FileSystemManager` → làm nóng `ThreadPoolManager` → nạp `TrashIndex`, `BookmarkStore` → lên lịch dọn thùng rác. |
| [`MainActivity`](app/src/main/java/com/zfile/manager/MainActivity.java) | Activity duy nhất chứa toàn bộ màn hình. Sở hữu Toolbar, breadcrumb và BottomNavigation (Browse / Categories / Search); điều hướng tới các màn overflow (Trash / Analyzer / Bookmarks / Settings) bằng Navigation Component. |

---

### 1. Tầng UI — `com.zfile.manager.ui`

Chỉ chịu trách nhiệm **hiển thị và nhận thao tác người dùng**, rồi `observe` LiveData để cập nhật giao diện. Không chứa logic nghiệp vụ.

#### `ui.fragment` — các màn hình chính

| Lớp | Vai trò |
|---|---|
| `FileBrowserFragment` | Màn duyệt file: danh sách, multi-select, copy/cut/paste, kéo-để-làm-mới, thanh tiến độ chuyển file |
| `CategoryFragment` | Lưới 6 nhóm phân loại (Ảnh/Video/Nhạc/Tài liệu/Tải về/APK) kèm số lượng |
| `CategoryDetailFragment` | Danh sách file trong một nhóm đã chọn |
| `SearchFragment` | Ô tìm kiếm + kết quả; chứa logic **debounce 300ms** |
| `TrashFragment` | Danh sách thùng rác: khôi phục / xoá vĩnh viễn / dọn sạch |
| `StorageAnalyzerFragment` | Biểu đồ tròn + top file/thư mục lớn nhất |
| `BookmarksFragment` | Danh sách dấu trang |
| `SettingsFragment` | Giao diện sáng/tối, số ngày giữ thùng rác |

#### `ui.adapter` — đổ dữ liệu cho RecyclerView

| Lớp | Vai trò |
|---|---|
| `FileListAdapter` | Hiển thị danh sách `FileItemComponent`, dùng **DiffUtil** để chỉ vẽ lại dòng thay đổi |
| `CategoryAdapter` | Lưới các nhóm phân loại |
| `BreadcrumbAdapter` | Thanh đường dẫn phân cấp ngang, mỗi đoạn bấm được để nhảy về |
| `TopFilesAdapter` | Danh sách file lớn nhất trong màn phân tích bộ nhớ |
| `BookmarkAdapter` | Danh sách dấu trang |

#### `ui.dialog` — hộp thoại theo **Builder pattern**

`CreateFileDialogBuilder`, `RenameDialogBuilder`, `PropertiesDialogBuilder`, `SortDialogBuilder`, `ConfirmDialogBuilder`, `ZipDialogBuilder`, `ExtractDialogBuilder`.

#### `ui.view` — View tự vẽ

| Lớp | Vai trò |
|---|---|
| [`ZoomableImageView`](app/src/main/java/com/zfile/manager/ui/view/ZoomableImageView.java) | ImageView hỗ trợ **pinch-zoom / pan / double-tap** bằng `Matrix`. Tự điều phối tranh chấp cử chỉ với `ViewPager2` cha (chưa zoom → nhường vuốt trang; đang zoom → giữ để pan) |
| `StoragePieChart` | Biểu đồ tròn tự vẽ trong `onDraw` bằng `drawArc` |

#### `ui.viewer` — trình xem file trong app

| Lớp | Vai trò |
|---|---|
| [`ViewerActivity`](app/src/main/java/com/zfile/manager/ui/viewer/ViewerActivity.java) | Activity host. **Định tuyến** theo loại file tới đúng fragment xem; loại không hỗ trợ thì bàn giao cho app ngoài |
| `BaseViewerFragment` | Lớp cha dùng chung cho các trình xem |
| `ImageViewerFragment` | Xem ảnh, vuốt qua lại bằng ViewPager2 + Glide |
| `TextViewerFragment` | Xem text/code, tô màu cú pháp |
| `VideoViewerFragment` | Phát video |
| `AudioPlayerFragment` | Phát nhạc (vòng đời `MediaPlayer`, `release()` ở `onDestroyView`) |
| `ui.viewer.adapter.ImagePageAdapter` | Adapter các trang ảnh cho ViewPager2 |

---

### 2. Tầng ViewModel — `com.zfile.manager.viewmodel`

Cầu nối theo **Observer pattern** giữa Repository và UI. Mỗi ViewModel phơi dữ liệu qua `LiveData` (chỉ đọc với UI) và đẩy tác vụ nặng xuống luồng nền.

| Lớp | Vai trò & điểm nhấn |
|---|---|
| [`FileBrowserViewModel`](app/src/main/java/com/zfile/manager/viewmodel/FileBrowserViewModel.java) | Trái tim của màn duyệt file: load thư mục, multi-select, copy/move/paste, tạo/đổi tên/xoá, nén/giải nén. **Mỗi tác vụ dài được cấp một `AtomicBoolean` cờ huỷ riêng** qua `AtomicReference` để huỷ không chéo |
| `CategoryViewModel` | Nạp danh sách theo nhóm phân loại từ MediaStore |
| `SearchViewModel` | Quản lý vòng đời truy vấn: huỷ truy vấn cũ, tạo cờ mới, trả kết quả theo lô, **chặn kết quả lỗi thời** |
| `TrashViewModel` | Liệt kê / khôi phục / xoá vĩnh viễn thùng rác |
| `StorageAnalyzerViewModel` | Khởi chạy phân tích bộ nhớ, phơi tiến độ từng giai đoạn |
| `BookmarkViewModel` | Thêm / xoá / liệt kê dấu trang |
| `SettingsViewModel` | Đọc/ghi theme và số ngày giữ thùng rác |

**Quy ước chung:** mỗi LiveData có cặp `private MutableLiveData _x` (ghi được, nội bộ) + `public LiveData getX()` (chỉ đọc, cho UI) — ép luồng dữ liệu một chiều.

---

### 3. Tầng Repository — `com.zfile.manager.repository`

Đóng vai **Facade**: che giấu sự phức tạp của tầng Service và **hợp nhất dữ liệu từ nhiều nguồn** về cùng một dạng để UI tái dùng.

| Lớp | Vai trò |
|---|---|
| [`FileRepository`](app/src/main/java/com/zfile/manager/repository/FileRepository.java) | Mặt tiền cho thao tác file. Đọc → uỷ quyền cho `DirectoryScannerService` rồi **bọc Decorator chain** (icon → màu → tag) tạo `FileItemComponent` sẵn-sàng-hiển-thị. Ghi → uỷ quyền cho `FileTransferService` |
| `MediaStoreRepository` | Truy vấn `MediaStore` qua `ContentResolver` để liệt kê file theo nhóm và tính tổng dung lượng mỗi nhóm |
| `TrashRepository` | Mặt tiền cho các thao tác thùng rác |
| `StorageAnalyzerRepository` | Mặt tiền cho phân tích bộ nhớ |


---

### 4. Tầng Service — `com.zfile.manager.service`

**POJO thuần** (không phải `android.app.Service`) thực hiện I/O thật. Mọi method **chạy đồng bộ trên luồng của người gọi** — bắt buộc lập lịch qua `ThreadPoolManager`.

| Lớp | Vai trò & điểm nhấn kỹ thuật |
|---|---|
| [`DirectoryScannerService`](app/src/main/java/com/zfile/manager/service/DirectoryScannerService.java) | Liệt kê file trong thư mục; lọc file ẩn; sắp xếp theo `SortCriteria` (Strategy/Comparator); đếm số con |
| [`FileTransferService`](app/src/main/java/com/zfile/manager/service/FileTransferService.java) | Copy/move/delete/create/rename. Đọc-ghi theo **khối 64KB**; kiểm tra cờ huỷ mỗi vòng; **tiết chế tiến độ 100ms**; chống vòng symlink bằng canonical path; tự đặt tên "(2)", "(3)" khi trùng |
| [`SearchService`](app/src/main/java/com/zfile/manager/service/SearchService.java) | Duyệt cây **theo chiều rộng (BFS)**, khớp tên không phân biệt hoa thường, trả **kết quả theo lô 50** để UI hiện dần |
| [`RecycleBinService`](app/src/main/java/com/zfile/manager/service/RecycleBinService.java) | Xoá mềm vào `<root>/.ZFileTrash/` (kèm `.nomedia`); khôi phục; xoá vĩnh viễn; **dọn theo lịch hằng ngày lúc 02:00** |
| `ArchiveService` | Nén/giải nén ZIP bằng `java.util.zip`, có tiến độ và huỷ |
| [`StorageAnalyzerService`](app/src/main/java/com/zfile/manager/service/StorageAnalyzerService.java) | Phân tích 3 bước: `StatFs` (tổng/trống) → MediaStore (theo nhóm) → BFS + **min-heap top-N** (file & thư mục lớn nhất) |

---

### 5. Tầng Core — `com.zfile.manager.core`

Các **Singleton (double-checked locking)** hạ tầng, dùng chung toàn app, khởi tạo trong `ZFileApplication`.

| Lớp | Vai trò |
|---|---|
| [`ThreadPoolManager`](app/src/main/java/com/zfile/manager/core/ThreadPoolManager.java) | Sở hữu 3 bể luồng: **`ioExecutor`** (core 4 / max 8 — copy, scan, zip), **`searchExecutor`** (1 luồng, **xoá hàng đợi mỗi lần nộp** để ưu tiên truy vấn mới nhất), **`scheduledExecutor`** (tác vụ định kỳ) |
| [`FileSystemManager`](app/src/main/java/com/zfile/manager/core/FileSystemManager.java) | Trạng thái hệ thống file toàn cục: danh sách ổ đĩa, thư mục gốc hiện tại, **clipboard copy/cut**, kiểm tra quyền truy cập |
| [`AppSettings`](app/src/main/java/com/zfile/manager/core/AppSettings.java) | Nguồn sự thật duy nhất cho cài đặt vô hướng (theme, sort, file ẩn, số ngày giữ rác). **Cache trong field `volatile` để đọc nhanh + lưu xuống SharedPreferences** |
| [`TrashIndex`](app/src/main/java/com/zfile/manager/core/TrashIndex.java) | Sổ ghi chép thùng rác: danh sách `TrashEntry` trong RAM, **đóng gói thành JSON lưu vào SharedPreferences** |
| [`BookmarkStore`](app/src/main/java/com/zfile/manager/core/BookmarkStore.java) | Sổ dấu trang (cấu trúc giống `TrashIndex`, nhưng tự lưu ngay sau mỗi thay đổi) |
| [`MimeTypeHelper`](app/src/main/java/com/zfile/manager/core/MimeTypeHelper.java) | Tiện ích **stateless**: nhận diện MIME, phân loại đuôi file thành `FileType`, tra icon resource |

---

### 6. Tầng Model — `com.zfile.manager.model`

Các **value object** (đối tượng giá trị) thuần.

| Lớp | Vai trò |
|---|---|
| [`FileItem`](app/src/main/java/com/zfile/manager/model/FileItem.java) | Mô tả **bất biến** của một file/thư mục: tên, đường dẫn, kích thước, ngày sửa, quyền, `FileType`, MIME |
| `FileType` | Enum loại file: FOLDER, IMAGE, VIDEO, AUDIO, DOCUMENT, ARCHIVE, APK, TEXT, UNKNOWN |
| `CategoryType` | Enum 6 nhóm phân loại MediaStore |
| `SortCriteria` | Enum kiểu sắp xếp (tên/kích thước/ngày/loại × tăng/giảm) |
| `StorageVolume` | Một ổ đĩa (Internal / SD Card) |
| `TransferProgress` | Bản chụp tiến độ chuyển file (pending/inProgress/completed/cancelled/failed) |
| `StorageAnalysis` | Kết quả phân tích bộ nhớ |
| `TrashEntry` | "Tấm phiếu" metadata một mục trong thùng rác (id UUID, đường dẫn gốc, tên đã đổi, thời điểm xoá) |
| `Bookmark` | Một dấu trang |
| `ArchiveEntry` | Một mục trong file nén |

#### `model.decorator` — **Decorator Pattern**

| Lớp | Vai trò |
|---|---|
| `FileItemComponent` | Interface chung (Component) |
| `BaseFileItem` | Bản gốc chưa trang trí, bọc một `FileItem` |
| `FileItemDecorator` | Lớp Decorator trừu tượng |
| `IconDecorator` | Khoác thêm icon resource |
| `ColorDecorator` | Khoác thêm màu tint theo loại file |
| `SystemTagDecorator` | Khoác thêm nhãn (vd "HIDDEN") |

> Mỗi `FileItem` được bọc dần: `BaseFileItem` → `IconDecorator` → `ColorDecorator` → (tuỳ chọn) `SystemTagDecorator`, tạo ra `FileItemComponent` đã sẵn sàng render mà **không sửa lớp `FileItem` gốc**.

---

### 7. Tầng Util — `com.zfile.manager.util`

| Lớp | Vai trò |
|---|---|
| `FileUtils` | Tiện ích định dạng kích thước, ngày tháng… |
| `PermissionHelper` | Xin và kiểm tra quyền truy cập bộ nhớ theo từng phiên bản Android |
| `IntentHelper` | Mở/chia sẻ file ra app ngoài qua `FileProvider` |
| `SyntaxHighlighter` | Tô màu cú pháp cho trình xem text/code |

---

## Các luồng chức năng tiêu biểu

**1. Sao chép/Dán** — `FileBrowserFragment` → `FileBrowserViewModel.pasteHere()` (tạo cờ huỷ mới) → `ThreadPoolManager` (luồng nền) → `FileRepository.copy()` → `FileTransferService` (đọc-ghi 64KB, kiểm tra huỷ, tiến độ 100ms) → tiến độ `postValue` lên `LinearProgressIndicator`.

**2. Xoá mềm** — `FileBrowserViewModel.deleteSelected()` → `RecycleBinService.moveToTrash()` (dời file vào `.ZFileTrash/` + tạo `TrashEntry`) → `TrashIndex.save()` (JSON → SharedPreferences).

**3. Đổi theme** — `SettingsFragment` → `AppSettings.setThemeMode()` (cache + persist) → `AppCompatDelegate.setDefaultNightMode()` (tự `recreate()`) → Activity dựng lại, đọc theme **trước khi vẽ** → không nháy.

**4. Phân tích bộ nhớ** — `StorageAnalyzerViewModel.analyze()` → `StorageAnalyzerService`: `StatFs` → MediaStore SUM theo nhóm → BFS + min-heap top-N → `StoragePieChart` + `TopFilesAdapter`.

**5. Tìm kiếm** — `SearchFragment` debounce 300ms → `SearchViewModel.submitQuery()` (huỷ truy vấn cũ, cờ mới) → `searchExecutor` (xoá hàng đợi) → `SearchService` BFS theo lô → guard chặn kết quả lỗi thời → `postValue`.



---

## Mẫu thiết kế (Design Patterns)

| Pattern | Áp dụng tại |
|---|---|
| **MVVM** | Toàn bộ kiến trúc UI ↔ ViewModel ↔ Repository |
| **Singleton (DCL)** | `ThreadPoolManager`, `FileSystemManager`, `AppSettings`, `TrashIndex`, `BookmarkStore`, `FileRepository`, `MediaStoreRepository` |
| **Facade** | Tầng Repository che giấu tầng Service |
| **Decorator** | `model.decorator` — khoác icon/màu/tag lên `FileItem` |
| **Observer** | `LiveData` ↔ Fragment |
| **Builder** | Các `*DialogBuilder` |
| **Adapter** | Các `*Adapter` cho RecyclerView/ViewPager2 |
| **Strategy** | `Comparator` theo `SortCriteria` |

---



## Quyền truy cập

| Quyền | Phạm vi |
|---|---|
| `MANAGE_EXTERNAL_STORAGE` | Android 11+ — quản lý toàn bộ file |
| `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` | Android 10 trở xuống (legacy) |
| `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO` | Android 13+ — quyền media chi tiết |

Chia sẻ file ra app ngoài qua `FileProvider` (authority `${applicationId}.fileprovider`).

---

## Cấu trúc thư mục

```
app/src/main/java/com/zfile/manager/
├── ZFileApplication.java          # Điểm khởi động, khởi tạo singleton
├── MainActivity.java              # Activity host + điều hướng
├── core/                          # Singleton hạ tầng
│   ├── ThreadPoolManager.java
│   ├── FileSystemManager.java
│   ├── AppSettings.java
│   ├── TrashIndex.java
│   ├── BookmarkStore.java
│   └── MimeTypeHelper.java
├── model/                         # Value objects
│   ├── FileItem.java · FileType.java · CategoryType.java
│   ├── SortCriteria.java · StorageVolume.java
│   ├── TransferProgress.java · StorageAnalysis.java
│   ├── TrashEntry.java · Bookmark.java · ArchiveEntry.java
│   └── decorator/                 # Decorator Pattern
│       ├── FileItemComponent.java · BaseFileItem.java
│       ├── FileItemDecorator.java · IconDecorator.java
│       ├── ColorDecorator.java · SystemTagDecorator.java
├── repository/                    # Facade
│   ├── FileRepository.java · MediaStoreRepository.java
│   ├── TrashRepository.java · StorageAnalyzerRepository.java
├── service/                       # POJO I/O
│   ├── DirectoryScannerService.java · FileTransferService.java
│   ├── SearchService.java · RecycleBinService.java
│   ├── ArchiveService.java · StorageAnalyzerService.java
├── viewmodel/                     # MVVM ViewModel
│   ├── FileBrowserViewModel.java · CategoryViewModel.java
│   ├── SearchViewModel.java · TrashViewModel.java
│   ├── StorageAnalyzerViewModel.java · BookmarkViewModel.java
│   └── SettingsViewModel.java
├── ui/
│   ├── fragment/                  # Màn hình
│   ├── adapter/                   # RecyclerView adapters
│   ├── dialog/                    # Builder dialogs
│   ├── view/                      # ZoomableImageView, StoragePieChart
│   └── viewer/                    # Trình xem ảnh/text/video/nhạc
└── util/                          # FileUtils, PermissionHelper, IntentHelper, SyntaxHighlighter
```

---



---

