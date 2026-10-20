# JIDU model guides

Every model this project supports has its own install page. Start with the page
for your unit: it lists the exact image files, the route that model uses, its
UART reference, and the parts that differ from its siblings.

The 6j01 pages lead with the SSH shortcut; the official 25.12.6 image is the
UART fallback. The 6j11 pages use the Mayank SSH method; UART is recovery only.
The shared procedure, with the reasoning behind each command, is
[Installing OpenWrt by hand](../../OPENWRT.md).

| Model | Family | Official OpenWrt | Route | UART pins | Status |
| --- | --- | --- | --- | --- | --- |
| [JIDU6101](JIDU6101.md) | MediaTek `6j01` | Yes (`jidu6101`) | SSH (primary), UART fallback | None published | SSH primary; official via UART |
| [JIDU6201](JIDU6201.md) | MediaTek `6j01` | Yes (`jidu6j01`) | SSH (primary), UART fallback | None published | SSH primary; official via UART |
| [JIDU6401](JIDU6401.md) | MediaTek `6j01` | Yes (`jidu6j01`) | SSH (primary), UART fallback | [photo](https://github.com/the-diy-daddy/6j01_6j11/blob/main/uart_pins/JIDU6401.jpg) | SSH primary; official via UART |
| [JIDU6601](JIDU6601.md) | MediaTek `6j01` | Yes (`jidu6j01`) | SSH (primary), UART fallback | None published | SSH primary; official via UART |
| [JIDU6701](JIDU6701.md) | MediaTek `6j01` | Yes (`jidu6j01`) | SSH (primary), UART fallback | None published | SSH primary; official via UART |
| [JIDU6111](JIDU6111.md) | Qualcomm `6j11` | No (community) | SSH (Mayank, primary), UART recovery | [photo](https://github.com/the-diy-daddy/6j01_6j11/blob/main/uart_pins/JIDU6111.jpg) | SSH primary; community |
| [JIDU6411](JIDU6411.md) | Qualcomm `6j11` | No (community) | SSH (Mayank, primary), UART recovery | [photo](https://github.com/the-diy-daddy/6j01_6j11/blob/main/uart_pins/JIDU6411.jpg) | SSH primary; community |
| [JIDU6611](JIDU6611.md) | Qualcomm `6j11` | No (community) | SSH (Mayank, primary), UART recovery | [photo](https://github.com/the-diy-daddy/6j01_6j11/blob/main/uart_pins/JIDU6611.jpg) | SSH primary; community |
| [JIDU6811](JIDU6811.md) | Qualcomm `6j11` | No (community) | SSH (Mayank, primary), UART recovery | [photo](https://github.com/the-diy-daddy/6j01_6j11/blob/main/uart_pins/JIDU6811.jpg) | SSH primary; community |
| [JIDU6911](JIDU6911.md) | Qualcomm `6j11` | No (community) | SSH (Mayank, primary), UART recovery | [photo](https://github.com/the-diy-daddy/6j01_6j11/blob/main/uart_pins/JIDU6911.jpg) | SSH primary; community |

For the 6j01 official image, use 25.12.6 or newer. Older 25.12 builds could
silently fail to write the SPI-NAND; the fix is
[PR 25122](https://github.com/openwrt/openwrt/pull/25122), included in the
25.12.6 images. The SSH route depends on the community build's version, so
check that before you flash.

Not sure which model you have? Detection changes nothing:

```sh
./flash.sh detect
```

Whichever page you land on, back up first. The backup is the only way back to
stock, and it holds the factory data that the flash does not recreate, including
`uboot_credentials.env` for the UART route.
