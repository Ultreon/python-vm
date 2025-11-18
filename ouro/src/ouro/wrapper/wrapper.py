import zipimport


def main() -> int:
    import json
    import urllib.request

    with open('ouro/wrapper/settings.json') as f:
        settings = json.load(f)

    ouro_distribution_url = settings['ouro-distribution-url']

    print("Downloading Ouro distribution...")
    urllib.request.urlretrieve(ouro_distribution_url, '.ouro/cache/ouro.pyz')
    print("Downloaded Ouro distribution")

    ziplib = zipimport.zipimporter('ouro/wrapper/ouro.pyz')
    ziplib.load_module('ouro')
    print("Loaded Ouro distribution")

    import ouro.core
    return ouro.core.main()
