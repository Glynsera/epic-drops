# Epic Drops

Epic Drops is an independent RuneLite plugin that displays
NPC loot information in a customizable fantasy-style tooltip.

## Features

- NPC drop information on hover
- Drop chance percentages
- Item icons
- Loot rarity colors
- Configurable tooltip size and transparency
- Adjustable number of visible drops
- Drop sorting
- Optional item icons and drop rates

## Data source

Epic Drops retrieves NPC drop information from the
Old School RuneScape Wiki using HTTPS requests to its public API.

When you hover over an NPC, the plugin may send that NPC's name
to the OSRS Wiki to retrieve its drop table. The request does not
include your RuneScape username, account credentials, or chat messages.
As with any internet request, the Wiki server may receive standard
connection information such as your IP address.

Retrieved drop information is cached locally during the plugin session.

Epic Drops is an independent community project and is
not affiliated with or endorsed by the OSRS Wiki team,
Jagex, or RuneLite.

## Known limitations

- A small number of item icons may not display.
- Some NPC variants may have different drop tables.
- Drop information depends on external Wiki availability.

## Development

Build the project:

    ./gradlew build

Run the development client:

    ./gradlew run

## License

MIT License. See LICENSE.