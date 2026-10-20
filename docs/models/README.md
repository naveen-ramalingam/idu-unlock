# JIDU model guides

Every model this project supports has its own install page. Start with the page
for your unit: it lists the exact image files, the route that model uses, the
UART reference, and the parts that differ from its siblings. The shared
procedure, with the reasoning behind each command, is
[Installing OpenWrt by hand](../../OPENWRT.md).

| Model | Family | Route | UART pins | Flashing status |
| --- | --- | --- | --- | --- |
| [JIDU6101](JIDU6101.md) | MediaTek `6j01` | SSH | None published | Unlock only. No completed flash is documented. |
| [JIDU6201](JIDU6201.md) | MediaTek `6j01` | SSH | None published | Shared 6j01 procedure, documented upstream. |
| [JIDU6401](JIDU6401.md) | MediaTek `6j01` | SSH | [photo](https://github.com/the-diy-daddy/6j01_6j11/blob/main/uart_pins/JIDU6401.jpg) | Shared 6j01 procedure, documented upstream. |
| [JIDU6601](JIDU6601.md) | MediaTek `6j01` | SSH | None published | Shared 6j01 procedure, documented upstream. |
| [JIDU6701](JIDU6701.md) | MediaTek `6j01` | SSH | None published | Shared 6j01 procedure, documented upstream. |
| [JIDU6111](JIDU6111.md) | Qualcomm `6j11` | SSH or UART | [photo](https://github.com/the-diy-daddy/6j01_6j11/blob/main/uart_pins/JIDU6111.jpg) | Shared 6j11 procedure, documented upstream. |
| [JIDU6411](JIDU6411.md) | Qualcomm `6j11` | SSH or UART | [photo](https://github.com/the-diy-daddy/6j01_6j11/blob/main/uart_pins/JIDU6411.jpg) | Shared 6j11 procedure, documented upstream. |
| [JIDU6611](JIDU6611.md) | Qualcomm `6j11` | SSH or UART | [photo](https://github.com/the-diy-daddy/6j01_6j11/blob/main/uart_pins/JIDU6611.jpg) | Shared 6j11 procedure, documented upstream. |
| [JIDU6811](JIDU6811.md) | Qualcomm `6j11` | SSH or UART | [photo](https://github.com/the-diy-daddy/6j01_6j11/blob/main/uart_pins/JIDU6811.jpg) | Shared 6j11 procedure, documented upstream. |
| [JIDU6911](JIDU6911.md) | Qualcomm `6j11` | SSH or UART | [photo](https://github.com/the-diy-daddy/6j01_6j11/blob/main/uart_pins/JIDU6911.jpg) | Shared 6j11 procedure, documented upstream. |

Not sure which model you have? Detection changes nothing:

```sh
./flash.sh detect
```

Whichever page you land on, back up first. The backup is the only way back to
stock, and it holds the factory data that the flash does not recreate.
