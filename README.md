# CE-Addon-StorageDrawers

将 [StorageDrawers](https://github.com/jaquadro/StorageDrawers) 移植到 CraftEngine。

支持木质、半深、压缩与镶框抽屉，管理器与连接件，镶框工作台，升级、钥匙、漏斗和磁力收集，以及带内容掉落、分类和中文界面。

## 运行环境

- Paper 26.3、Java 25、CraftEngine 26.10-SNAPSHOT。
- CoreProtect 可选，提供容器日志与 inspect 支持。

插件需要搭配对应的 CE 资源包使用。正面数量默认开启，顶部瞄准悬浮默认关闭，可在配置文件中调整。

## 源码与 API

源码位于 `src/main`。编译需要对应版本的 Paper、CraftEngine 与 CoreProtect API。

通过 `StorageDrawersApi.get()` 或 Bukkit ServicesManager 获取 API，提供抽屉查询、物品和数量 get/set、升级与状态设置及事件。API 在服务器主线程调用，仅访问已加载区块。

## 许可证

沿用仓库的 [GPL-3.0](LICENSE)，StorageDrawers 上游代码和资源保留 [MIT 声明](src/main/resources/licenses/StorageDrawers-MIT.txt)。
