# /// script
# dependencies = [
#   "click>=8.2.1",
#   "lxml>=6.0.2",
# ]
# ///
from pathlib import Path
import click
import re
from collections.abc import Sequence
from lxml import etree


@click.group()
def main() -> None: ...


@main.command()
@click.option("--indent", type=int)
@click.option("--jfx-version", type=str)
@click.argument(
    "paths",
    type=click.Path(
        exists=True,
        dir_okay=False,
        writable=True,
        readable=True,
        path_type=Path,
    ),
    nargs=-1,
    metavar="[FXML]...",
)
def format(
    *,
    indent: int | None,
    jfx_version: str | None,
    paths: Sequence[Path],
) -> None:
    for path in paths:
        try:
            tree = etree.parse(path)
        except Exception as e:
            message = f"{path!s} is not xml"
            raise ValueError(message) from e

        if indent is not None:
            etree.indent(tree.getroot(), " " * indent)

        text = etree.tostring(
            tree, encoding="utf-8", xml_declaration=True, pretty_print=True
        )

        if jfx_version is not None:
            # Replace jfx version
            text = re.sub(
                rb"xmlns=\"http://javafx.com/javafx/\d+(?:\.\d+)*\"",
                f'xmlns="http://javafx.com/javafx/{jfx_version}"'.encode("utf-8"),
                text,
            )

        for need_empty_line in [
            rb"^<\?import",
            rb"^<[^!?]",
        ]:
            text = re.sub(
                need_empty_line,
                rb"\n\g<0>",
                text,
                count=1,
                flags=re.MULTILINE,
            )

        path.write_bytes(text)


if __name__ == "__main__":
    main()
