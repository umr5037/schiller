<?xml version="1.0"?>
<xsl:stylesheet
  xmlns:xd="http://www.pnp-software.com/XSLTdoc"
  xmlns:edate="http://exslt.org/dates-and-times"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:tei="http://www.tei-c.org/ns/1.0"
  exclude-result-prefixes="tei edate xd" version="2.0">
  
  <xd:doc type="stylesheet">
    <xd:short>
      Feuille de style de préparation de fichiers TEI à l'importation
      TXM dans un format xml simple. 
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
  
	<!-- Listes des balises par type de traitement. -->
	
	<!--Par défaut, les balises sont conservées (les espaces de nommage sont
	respectés). On peut utiliser les paramètres ci-dessous pour indiquer les balises
	à supprimer en conservant ou non leur contenu--> 
	
	<!-- balises à supprimer avec leur contenu -->
	
	<xsl:param name="deleteAll">teiHeader|facsimile</xsl:param>
	
	<xsl:template match="*[matches(name(),concat('^',$deleteAll,'$'))]"/>
	
	<!-- balises à supprimer en conservant le contenu-->
	
	<xsl:param name="deleteTag"></xsl:param>
	
	<xsl:template match="*[matches(name(),concat('^',$deleteTag,'$'))]">
		<xsl:apply-templates/>
	</xsl:template>
	
  
  <xsl:template match="*">
  	<xsl:copy>
  		<xsl:apply-templates select="*|@*|processing-instruction()|comment()|text()"/>	
  	</xsl:copy>
  </xsl:template>

  <xsl:template match="@*|comment()">
    <xsl:copy/>
  </xsl:template>
	
	<xsl:template match="processing-instruction()"/>
	
	<xsl:template match="text()"><xsl:value-of select="."/></xsl:template>

	<!-- On supprime le teiHeader  pour l'import xml-w -->
	

	<xsl:template match="tei:facsimile">    
		<!--<xsl:copy-of select="."/>-->    
	</xsl:template>
	
	<xsl:template match="tei:w">
	  <xsl:variable name="wordsindiv">
	    <xsl:number from="tei:div" level="any"/>
	  </xsl:variable>
	  <xsl:if test="$wordsindiv mod 300 = 0">
	    <xsl:element name="pb" namespace="http://www.tei-c.org/ns/1.0">
	      <xsl:attribute name="n">
	        <xsl:value-of select="concat(count(preceding::tei:div)+1,'-',($wordsindiv div 300) + 1)"/>
	      </xsl:attribute>
	    </xsl:element>
	  </xsl:if>
	  
	  <xsl:element name="w" namespace="http://www.tei-c.org/ns/1.0">
	  	<xsl:if test="@id">
	  		<xsl:attribute name="orig-id"><xsl:value-of select="@id"/></xsl:attribute>
	  	</xsl:if>
	    <xsl:apply-templates select="@*[not(local-name()='id')]"/>
	    <xsl:for-each select="child::tei:moot/@*">
	      <xsl:attribute name="moot-{local-name(.)}"><xsl:value-of select="."/></xsl:attribute>
	    </xsl:for-each>
	    <xsl:for-each select="child::tei:xlit/@*">
	    	<xsl:variable name="attname"><xsl:value-of select="replace(lower-case(local-name(.)),'([0-9])$','$1x')"/></xsl:variable>
	      <xsl:attribute name="xlit-{$attname}"><xsl:value-of select="."/></xsl:attribute>
	    </xsl:for-each>
	    <xsl:value-of select="."/>
	  </xsl:element>
	</xsl:template>
  
  <xsl:template match="tei:div">
    <xsl:element name="pb" namespace="http://www.tei-c.org/ns/1.0">
      <xsl:attribute name="n"><xsl:value-of select="concat(count(preceding::tei:div) + 1,'-1')"/></xsl:attribute>
    </xsl:element>
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template match="tei:text//tei:p">
    <xsl:copy>
      <xsl:attribute name="n"><xsl:number from="tei:text"/></xsl:attribute>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

	<!-- ancien filtre pour les noeuds texte, n'est plus nécessaire pour l'import xml/w TXM 0.5 -->

  <!--<xsl:template match="text()">
  	<xsl:analyze-string select="." regex="&amp;lt;|&amp;gt;|&lt;|&gt;|-\-">
      <xsl:matching-substring>
        <xsl:choose>
        	<xsl:when test="matches(.,'&amp;lt;')">
        		<xsl:text>‹</xsl:text>
        	</xsl:when>
        	<xsl:when test="matches(.,'&amp;gt;')">
        		<xsl:text>›</xsl:text>
        	</xsl:when>
          <xsl:when test="matches(.,'&lt;')">
            <xsl:text>‹</xsl:text>
          </xsl:when>
          <xsl:when test="matches(.,'&gt;')">
            <xsl:text>›</xsl:text>
          </xsl:when>
          <!-\- pose problème si se trouve dans les notes transformés en commentaires xml par le tokeniseur -\->
          <xsl:when test="matches(.,'-\-')">
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
  </xsl:template>-->

</xsl:stylesheet>