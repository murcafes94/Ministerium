#!/usr/bin/env python3
"""Ministerium 3.1 ritual package: Ritual Romano + selected Bendicional rites."""

import build_liturgiapapal_rituals as base

base.SOURCES.update({
    "blessing_family": {
        "title": "Bendicional — Bendición de una familia",
        "url": "https://www.liturgiapapal.org/attachments/article/966/Familias.pdf",
        "required": "BENDICIÓN DE UNA FAMILIA",
    },
    "blessing_house": {
        "title": "Bendicional — Bendición de una nueva casa",
        "url": "https://www.liturgiapapal.org/attachments/article/967/Casa2.pdf",
        "required": "BENDICIÓN DE UNA NUEVA CASA",
    },
    "blessing_sick": {
        "title": "Bendicional — Bendición de los enfermos",
        "url": "https://www.liturgiapapal.org/attachments/article/966/Enfermos.pdf",
        "required": "BENDICIÓN DE LOS ENFERMOS",
    },
    "blessing_travel": {
        "title": "Bendicional — Bendición de los que van a emprender un viaje",
        "url": "https://www.liturgiapapal.org/attachments/article/966/Viaje.pdf",
        "required": "EMPRENDER UN VIAJE",
    },
    "blessing_transport": {
        "title": "Bendicional — Bendición de lo relacionado con desplazamientos humanos",
        "url": "https://www.liturgiapapal.org/attachments/article/967/Desplazamiento.pdf",
        "required": "DESPLAZAMIENTOS HUMANOS",
    },
    "blessing_liturgical_objects": {
        "title": "Bendicional — Objetos usados en celebraciones litúrgicas",
        "url": "https://www.liturgiapapal.org/attachments/article/968/Celebrac.pdf",
        "required": "BENDICIÓN DE OBJETOS QUE SE USAN EN LAS",
    },
    "blessing_water": {
        "title": "Bendicional — Bendición del agua fuera de la Misa",
        "url": "https://www.liturgiapapal.org/attachments/article/968/Agua.pdf",
        "required": "bendición del agua tiene lugar fuera",
    },
    "blessing_rosaries": {
        "title": "Bendicional — Bendición de los rosarios",
        "url": "https://www.liturgiapapal.org/attachments/article/968/Rosario.pdf",
        "required": "BENDICIÓN DE LOS ROSARIOS",
    },
    "blessing_animals": {
        "title": "Bendicional — Bendición de los animales",
        "url": "https://www.liturgiapapal.org/attachments/article/967/Animales.pdf",
        "required": "BENDICIÓN DE LOS ANIMALES",
    },
})

if __name__ == "__main__":
    try:
        base.build()
    except Exception as error:
        print(f"ERROR: {error}", file=base.sys.stderr)
        raise SystemExit(1)
