# Mete Economy

Minecraft 1.21.11 Fabric ekonomi modu — TL para birimi, eşya alış/satış, yuksek seviye büyü ve OP yönetim menüsü.

## Kurulum

1. Fabric Loader 0.19.5+ ve Fabric API kurun (Minecraft **1.21.11**).
2. Derlenmiş `mete-economy-1.1.3.jar` dosyasını `mods` klasörüne koyun.
3. Sunucu veya tek oyunculu dunyayi başlatın.

## Komutlar

| Komut | Açıklama | Yetki |
|-------|----------|-------|
| `/para` | Bakiyeni gosterir | Herkes |
| `/para ver <oyuncu> <miktar>` | Para ver | OP |
| `/para al <oyuncu> <miktar>` | Para al | OP |
| `/para set <oyuncu> <miktar>` | Bakiyeyi ayarla | OP |
| `/al <eşya> [adet]` | Magazadan satin al (örn. `/al diamond 5`) | Herkes |
| `/sat [adet]` | Elindeki eşyayi sat | Herkes |
| `/fiyat [eşya]` | Alis/satış fiyatini goster | Herkes |
| `/buyenchant <büyü> <seviye>` | Elindeki eşyaya büyü uygula (örn. `/buyenchant sharpness 50`) | Herkes |
| `/ekonomimenu` | OP yönetim GUI | OP |

## Ozellikler

- Kalıcı bakiye ve fiyatlar (`world/mete_economy.json`)
- Varsayilan eşya ve büyü fiyatları
- OP menüsü ile fiyat ve sistem ayarları
- Buyu sistemi (max seviye ayarlanabilir, varsayilan 1000)

## Derleme

```bash
./gradlew build
```

Çıktı: `build/libs/mete-economy-1.1.3.jar`

## Lisans

MIT
