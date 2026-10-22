# Loads an XML file whole, refusing any DTD.
# A save can come from another player, and a DTD's nested entities can expand a small file past memory.
# XStream never writes a DTD, so refusing one costs no real save.
function Read-XmlDocument {

    param([string] $Path)

    $settings = [System.Xml.XmlReaderSettings]::new()
    $settings.DtdProcessing = [System.Xml.DtdProcessing]::Prohibit

    $document = [System.Xml.XmlDocument]::new()
    $reader = [System.Xml.XmlReader]::Create($Path, $settings)

    try {
        $document.Load($reader)

    } finally {
        $reader.Dispose()
    }

    return $document
}
