# CE-Addon-StorageDrawers

�� [StorageDrawers](https://github.com/jaquadro/StorageDrawers) ��ֲ�� CraftEngine�������� Paper 26.3��Java 25��CraftEngine 26.10-SNAPSHOT��

����ľ�ʣ����ѹ���������롢�����������Ӽ���˫�������̨��������Կ�ס�©���ʹ����ռ��������ݵ��䡢���༰���� GUI���� 100 �෽�顢159 ����Ʒ��139 ���䷽��

## ��װ

1. ���������� `CE-Addon-StorageDrawers.jar` ���� `plugins`���ɰ� `StorageDrawers-CE.jar` Ӧ�Ƴ���Ŀ¼��
2. �� `build/stage/CraftEngine/resources/storagedrawers` ���� CE �� `resources`����������Ŀ����ĺϲ���Դ�������ء�
3. �� CE ������� StorageDrawers ������ʹ�á�����С����Ĭ�Ͽ�����������׼����Ĭ�Ϲرգ������ڲ�������е�����

CoreProtect Ϊ��ѡ������֧��������־�� inspect������ CE ����ʵ�� NBT �ع�����֧�ַ�Χ��Paper 26.3 ��Ҫƥ��� CoreProtect ������

ʹ�� PacketEvents ʱ���� CE �� `misc.inject-packetevents`������ Grim ״̬ӳ���޸�λ�� Cendreal-AC����ظĶ����� [compatibility/grim/cendreal-ac-ce.patch](compatibility/grim/cendreal-ac-ce.patch)��Ӧ�õ���Ӧά����֧�����¹��������������ף���Ϊ����ٷ� Grim �汾��ŵ���ݡ�

## ����

��Ҫ PowerShell 7��JDK 25��Node.js 22+��Python 3���Լ����������� Paper 26.3 �����Ŀ¼��CraftEngine JAR ���� `CraftEngine/libs` ���桢CoreProtect ������������Դ���ɻ���Ҫ���� Minecraft 26.3 �ͻ��� JAR �������� `assets` Ŀ¼��Ĭ������ `34`��������������ˡ��ͻ��˺����� JAR ����Դ���ṩ��

```powershell
npm install
python -m pip install -r requirements.txt
git clone https://github.com/jaquadro/StorageDrawers.git vendor/StorageDrawers
git -C vendor/StorageDrawers checkout 62a33ab1aa3e79e0c13e149f34fca4d30f07df8a

./Build.ps1 -PaperHome 'D:/server' `
  -CraftEngineJar 'D:/server/plugins/CraftEngine.jar' `
  -CoreProtectJar 'D:/deps/CoreProtect-24.1.jar' `
  -MinecraftClientJar 'D:/minecraft/26.3-client.jar' `
  -MinecraftAssets 'D:/minecraft/assets'
```

�� JDK ���� PATH������ `-JavaHome`��Python ���� `-Python` ָ��������Դ��λ�ÿ��� `-StorageDrawersSource` ָ����ֻ���� Java ʱʹ�� `-SkipPack`��

�����`build/CE-Addon-StorageDrawers.jar`��`build/CE-Addon-StorageDrawers-api.jar` �� CE ��ԴĿ¼��API JAR ��������������룬������ `plugins`��

## API ��ṹ

API �� Bukkit `ServicesManager` �� `StorageDrawersApi.get()` ��ȡ���������������� `CE-Addon-StorageDrawers` ��������������·�����ӿ��ڷ��������̵߳��ã��������Ѽ������顣

```java
StorageDrawersApi api = StorageDrawersApi.get();
if (api != null) {
    api.getDrawer(location).ifPresent(drawer -> {
        int count = drawer.getCount(0);
        drawer.setCount(0, count + 1);
    });
}
```

`api` �ṩ���ա�get/set ���¼���`module` �����洢���Զ������������˵��ͺϳɣ�`event` ���������`render` ������ʾ��`compatibility` �Խ� CE �� CoreProtect��`lifecycle` ����װ����ͷš���ͨ���벻��ѯ�����簴�Ѽ��ؽڵ�����˱仯���棬�ռ���������ʱ���ߡ�

������ Forge/Fabric ģ��浵��FTB/CoFH ģ��ר����������Ϊ Paper �����ṩ��������֤���Ǵ�ȡ�������طš�������GUI��������Ⱦ�������е������غ㣬����������������˺;�Э�����ա�

## ����֤

���òֿ����� [GPL-3.0](LICENSE)��StorageDrawers ���δ�������Դ���� [MIT ����](src/main/resources/licenses/StorageDrawers-MIT.txt)�����ι̶��ύ���������CraftEngine��CoreProtect��Minecraft ������������ѭ��������֤��
