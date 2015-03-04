<?xml version="1.0"?>
<xsl:stylesheet
  xmlns:xd="http://www.pnp-software.com/XSLTdoc"
  xmlns:edate="http://exslt.org/dates-and-times"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:tei="http://www.tei-c.org/ns/1.0"
  exclude-result-prefixes="xd edate xsl tei" version="2.0">
  
  <xd:doc type="stylesheet">
    <xd:short>
      Feuille de style de préparation de fichiers TEI à l'importation
      TXM dans un format xml simple. Adapté au corpus Schiller 
    </xd:short>
    <xd:detail>
      This stylesheet is free software; you can redistribute it and/or
      modify it under the terms of the GNU Lesser General Public
      License as published by the Free Software Foundation; either
      version 3 of the License, or (at your option) any later version.
      
      This stylesheet is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
      Lesser General Public License for more details.
      
      You should have received a copy of GNU Lesser Public License with
      this stylesheet. If not, see http://www.gnu.org/licenses/lgpl.html
    </xd:detail>
    <xd:author>Alexei Lavrentiev alexei.lavrentev@ens-lyon.fr</xd:author>
    <xd:copyright>2012, CNRS / ICAR (ICAR3 LinCoBaTO)</xd:copyright>
  </xd:doc>
  

  <xsl:output method="xml" encoding="utf-8" omit-xml-declaration="no"/>
  
  
  <xsl:template match="tei:TEI">
      <xsl:apply-templates/>  
  </xsl:template>

  
  <xsl:template match="*">
        <xsl:element namespace="http://www.tei-c.org/ns/1.0" name="{local-name(.)}">
          <xsl:apply-templates select="*|@*|processing-instruction()|comment()|text()"/>
        </xsl:element>
  </xsl:template>

  <xsl:template match="@*|comment()">
    <xsl:copy/>
  </xsl:template>

  <xsl:template match="text()">
  	<xsl:analyze-string select="." regex="&amp;lt;|&amp;gt;|&amp;|&lt;|&gt;|--|\*(\w+)">
      <xsl:matching-substring>
        <xsl:choose>
        	<xsl:when test="matches(.,'&amp;lt;')">
        		<xsl:text>‹</xsl:text>
        	</xsl:when>
        	<xsl:when test="matches(.,'&amp;gt;')">
        		<xsl:text>›</xsl:text>
        	</xsl:when>
<!--          <xsl:when test="matches(.,'&amp;')">
            <!-\-<expan xmlns="http://www.tei-c.org/ns/1.0">and</expan>-\->
          	<xsl:text>&amp;amp;</xsl:text>
          </xsl:when>-->
          <xsl:when test="matches(.,'&lt;')">
            <xsl:text>‹</xsl:text>
          </xsl:when>
          <xsl:when test="matches(.,'&gt;')">
            <xsl:text>›</xsl:text>
          </xsl:when>
          <!-- pose problème si se trouve dans les notes transformés en commentaires xml par le tokeniseur -->
          <xsl:when test="matches(.,'--')">
            <xsl:text> - </xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="."/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:matching-substring>
      <xsl:non-matching-substring>         
            <xsl:value-of select="."/>
      </xsl:non-matching-substring>      
    </xsl:analyze-string>
  </xsl:template>
  

  <xsl:template match="processing-instruction()"/>

<!-- On supprime le teiHeader  pour l'import xml-w -->
  
  <xsl:template match="tei:teiHeader">    
      <!--<xsl:copy-of select="."/>-->    
  </xsl:template>
  
  <xsl:template match="tei:facsimile">    
    <!--<xsl:copy-of select="."/>-->    
  </xsl:template>
	
<!--	<xsl:template match="tei:teiHeader//tei:title/text()">
		<xsl:value-of select="replace(.,'&quot;','&apos;&apos;&apos;&apos;')"></xsl:value-of>
	</xsl:template>
-->	

<!-- special Schiller  -->
  
  <xsl:template match="tei:text">
    <xsl:copy>
      <xsl:apply-templates select="tei:front|tei:body"/>
      <back xmlns="http://www.tei-c.org/ns/1.0">
        <div type="notes" xmlns="http://www.tei-c.org/ns/1.0">
          <xsl:apply-templates select="//tei:note" mode="final"></xsl:apply-templates>
        </div>
      </back>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template match="tei:note">
    <xsl:variable name="note-n"><xsl:value-of select="count(preceding::tei:note)+1"/></xsl:variable>
    <w xmlns="http://www.tei-c.org/ns/1.0">
      <xsl:attribute name="note"><xsl:value-of select="normalize-space(.)"/></xsl:attribute>
      <xsl:value-of select="concat('[Note',$note-n,']')"/>
    </w>
  </xsl:template>
  
  <xsl:template match="tei:note" mode="final">
    <xsl:variable name="note-n"><xsl:value-of select="count(preceding::tei:note)+1"/></xsl:variable>
    <pb xmlns="http://www.tei-c.org/ns/1.0" n="{concat('Note ',$note-n)}"/>
    <ab type="note" n="{$note-n}" xmlns="http://www.tei-c.org/ns/1.0"><xsl:apply-templates/></ab>
  </xsl:template>
  
  <xsl:template match="tei:div">
    <xsl:variable name="position" select="sum(count(preceding-sibling::tei:div) + 1)"/>
    <xsl:copy>
      <xsl:if test="child::tei:head">
        <xsl:attribute name="titre"><xsl:value-of select="normalize-space(child::tei:head)"/></xsl:attribute>
      </xsl:if>
      <xsl:if test="child::tei:head//tei:date[@when]">
        <xsl:attribute name="date"><xsl:attribute name="date"><xsl:value-of select="child::tei:head//tei:date/@when"/></xsl:attribute></xsl:attribute>
      </xsl:if>
      <xsl:attribute name="type">Brief</xsl:attribute>
      <xsl:attribute name="xml-id" select="concat($text,format-number($position, '00'))"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template match="tei:pb[ancestor::tei:note]"/>
  
  <!-- Maud et Valérie -->
  <xsl:param name="text">Baest</xsl:param>
  
  
  <xsl:template match="tei:p">
    <xsl:variable name="position" select="sum(count(preceding-sibling::tei:p) + 1)"/>
    <p n="{$position}">
      <xsl:apply-templates/>
    </p>
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
</xsl:stylesheet>