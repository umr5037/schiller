<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet  xmlns="http://www.tei-c.org/ns/1.0" xmlns:tei="http://www.tei-c.org/ns/1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="2.0" exclude-result-prefixes="tei">
   <xsl:output method="xml" encoding="UTF-8"/>
    <xsl:param name="text">Baest</xsl:param>
    <xsl:template match="/">
       <xsl:apply-templates/>
    </xsl:template>
    
    <xsl:template match="tei:div">
        <xsl:variable name="position" select="sum(count(preceding-sibling::tei:div) + 1)"/>
        <div type="Brief" xml-id="{concat($text,format-number($position, '00'))}">
            <xsl:apply-templates/>
        </div>
    </xsl:template>
    
    <xsl:template match="tei:p">
        <xsl:variable name="position" select="sum(count(preceding-sibling::tei:p) + 1)"/>
        <p n="{$position)}">
            <xsl:apply-templates/>
        </p>
    </xsl:template>
    
    <xsl:template match="*">
        <xsl:copy>
            <xsl:copy-of select="@*"/>
            <xsl:apply-templates />
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="text()">
        <xsl:analyze-string select="." regex="\[(\d+)\]">
            <xsl:matching-substring><pb type="reclam" n="{regex-group(1)}" /></xsl:matching-substring>
            <xsl:non-matching-substring><xsl:value-of select="." /></xsl:non-matching-substring>
        </xsl:analyze-string>
    </xsl:template>
    
    <xsl:template match="@rend">
        <xsl:analyze-string select="." regex="bold">
            <xsl:matching-substring></xsl:matching-substring>
            <xsl:non-matching-substring>
               <xsl:attribute name="rend" select="normalize-space(.)" />
            </xsl:non-matching-substring>
        </xsl:analyze-string>
    </xsl:template>
    
    <xsl:template match="tei:anchor" />
    <xsl:template match="tei:hi[@rend='bold']"><xsl:apply-templates /></xsl:template>
    <xsl:template match="tei:hi">
        <emph>
            <xsl:apply-templates select="@rend" />
            <xsl:apply-templates />
        </emph>
    </xsl:template>
    <xsl:template match="tei:index" />
    <xsl:template match="tei:note[@place='end']" />
    <xsl:template match="tei:note">
        <note>##&nbsp;<xsl:apply-templates />&nbsp;##</note> 
    </xsl:template>
</xsl:stylesheet>