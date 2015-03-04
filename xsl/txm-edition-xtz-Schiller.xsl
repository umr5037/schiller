<?xml version="1.0"?>
<xsl:stylesheet xmlns:edate="http://exslt.org/dates-and-times"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:tei="http://www.tei-c.org/ns/1.0" xmlns:txm="http://textometrie.org/1.0"
                exclude-result-prefixes="#all" version="2.0">
                
	<xsl:output method="xml" encoding="UTF-8" omit-xml-declaration="no" indent="no"  doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN" doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"/>
                
                <xsl:strip-space elements="*"/>
                
                <xsl:variable name="inputtype">
                	<xsl:choose>
                		<xsl:when test="//tei:w//txm:form">xmltxm</xsl:when>
                		<xsl:otherwise>xmlw</xsl:otherwise>
                	</xsl:choose>
                </xsl:variable>
                
                <xsl:template match="/">
                	<html>
                		<head>
                			<title><xsl:value-of select="//tei:text[1]/@id"/></title>
                			<meta http-equiv="Content-Type" content="text/html;charset=UTF-8"/>
                			<link rel="stylesheet" media="all" type="text/css" href="css/bvhepistemon2014.css" />
<!--                			<title>
                				<xsl:if test="$author[not(contains(.,'anonym'))]">
                					<xsl:value-of select="$author"/><xsl:text> : </xsl:text>
                				</xsl:if>
                				<xsl:value-of select="$title-normal"/>
                			</title>                                                                -->
                		</head>
                			<xsl:apply-templates select="descendant::tei:text"/>
                	</html>
                </xsl:template>

<xsl:template match="tei:text">
	<body>
		<a class="txm-page" title="1"/>
		<div class="metadata-page">
			<h1><xsl:value-of select="@id"></xsl:value-of></h1>
			<br/>
			<table>
				<xsl:for-each select="@*">
					<tr>
						<td><xsl:value-of select="name()"/></td>
						<td><xsl:value-of select="."/></td>
					</tr>
				</xsl:for-each>
			</table>
		</div>
		<xsl:apply-templates/>		
	</body>
</xsl:template>

                <xsl:template match="*">
                                <xsl:choose>
                                	<xsl:when test="descendant::tei:p|descendant::tei:ab">
                                		<div>
                                			<xsl:call-template name="addClass"/>
                                			<xsl:apply-templates/></div>
                                		<xsl:text>&#xa;</xsl:text>
                                	</xsl:when>
                                	<xsl:otherwise><span>
                                		<xsl:call-template name="addClass"/>
                                		<xsl:if test="self::tei:add[@del]">
                                			<xsl:attribute name="title"><xsl:value-of select="@del"/></xsl:attribute>
                                		</xsl:if>
                                		<xsl:apply-templates/></span>
                                	<xsl:call-template name="spacing"/>
                                	</xsl:otherwise>
                                </xsl:choose>
                </xsl:template>
                
                <xsl:template match="@*|processing-instruction()|comment()">
                                <!--<xsl:copy/>-->
                </xsl:template>
                
<!--                <xsl:template match="comment()">
                                <xsl:copy/>
                </xsl:template>
-->                
                <xsl:template match="text()">
                                <xsl:value-of select="normalize-space(.)"/>
                </xsl:template>
                
                <xsl:template name="addClass">
                	<xsl:attribute name="class">
                		<xsl:value-of select="local-name(.)"/>
                		<xsl:if test="@type"><xsl:value-of select="concat('-',@type)"/></xsl:if>
                		<xsl:if test="@subtype"><xsl:value-of select="concat('-',@subtype)"/></xsl:if>
                		<xsl:if test="@rend"><xsl:value-of select="concat('-',@rend)"/></xsl:if>
                	</xsl:attribute>                	
                </xsl:template>
                
                <xsl:template match="tei:p|tei:ab|tei:lg">
                	<p>
                		<xsl:call-template name="addClass"/>
                		<xsl:if test="@n">
                			<span class="verseline"><span class="verselinenumber"><xsl:value-of select="concat('§ ',@n)"/></span></span>
                		</xsl:if>
                		<xsl:apply-templates/>
                	</p>
                	<xsl:text>&#xa;</xsl:text>
                </xsl:template>
	
	<xsl:template match="tei:head">
		<h2>
			<xsl:call-template name="addClass"/>
			<xsl:apply-templates/>
		</h2>
	</xsl:template>
                
	<xsl:template match="//tei:lb">
		<xsl:variable name="lbcount">
			<xsl:choose>
				<xsl:when test="ancestor::tei:ab"><xsl:number from="tei:ab" level="any"/></xsl:when>
				<xsl:when test="ancestor::tei:p"><xsl:number from="tei:p" level="any"/></xsl:when>
				<xsl:otherwise>999</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:if test="@rend='hyphen(-)'"><span class="hyphen">-</span></xsl:if>
		<xsl:if test="@rend='hyphen(=)'"><span class="hyphen">=</span></xsl:if>
		<xsl:if test="not($lbcount=1) or preceding-sibling::node()[matches(.,'\S')]"><br/><xsl:text>&#xa;</xsl:text></xsl:if>
		<xsl:if test="@n and not(@rend='prose')">
			<xsl:choose>
				<xsl:when test="matches(@n,'^[0-9]*[05]$')">
					<!--<a title="{@n}" class="verseline" style="position:relative"> </a>-->
					<span class="verseline"><span class="verselinenumber"><xsl:value-of select="@n"/></span></span>
				</xsl:when>
				<xsl:when test="matches(@n,'[^0-9]')">
					<!--<a title="{@n}" class="verseline" style="position:relative"> </a>-->
					<span class="verseline"><span class="verselinenumber"><xsl:value-of select="@n"/></span></span>
				</xsl:when>
				<xsl:otherwise>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
	</xsl:template>
	
	<!-- Page breaks -->                
	<xsl:template match="//tei:pb[not(following-sibling::tei:cb)]">
		<xsl:variable name="editionpagetype">
			<xsl:choose>
				<xsl:when test="ancestor::tei:ab">editionpageverse</xsl:when>
				<xsl:otherwise>editionpage</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:variable name="pagenumber">
			<xsl:choose>
				<xsl:when test="@n"><xsl:value-of select="@n"/></xsl:when>
				<xsl:when test="@facs"><xsl:value-of select="substring-before(@facs,'.')"/></xsl:when>
				<xsl:otherwise>[NN]</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		
		<xsl:variable name="page_id"><xsl:value-of select="count(preceding::tei:pb)"/></xsl:variable>
		
		<xsl:if test="//tei:note[not(@place='inline') and not(matches(@type,'intern|auto'))][following::tei:pb[1][count(preceding::tei:pb) = $page_id]]">
			<xsl:text>&#xa;</xsl:text>
			<br/>
			<br/>			
			<span style="display:block;border-top-style:solid;border-top-width:1px;border-top-color:gray;padding-top:5px">                                                
				<xsl:for-each select="//tei:note[not(@place='inline') and not(matches(@type,'intern|auto'))][following::tei:pb[1][count(preceding::tei:pb) = $page_id]]">
					<xsl:variable name="note_count"><xsl:value-of select="count(preceding::tei:note[not(@place='inline')]) + 1"/></xsl:variable>
					<!--<p><xsl:value-of select="$note_count"/>. <a href="#noteref_{$note_count}" name="note_{$note_count}">[<xsl:value-of select="preceding::tei:cb[1]/@xml:id"/>, l. <xsl:value-of select="preceding::tei:lb[1]/@n"/>]</a><xsl:text> </xsl:text> <xsl:value-of select="."/></p>-->
					<span class="note">
						<span style="position:absolute;left:-30px"><a href="#noteref_{$note_count}" name="note_{$note_count}"><xsl:value-of select="$note_count"/></a>. </span>
						<xsl:apply-templates mode="#current"/>
					</span>                                                                
				</xsl:for-each></span><xsl:text>&#xa;</xsl:text>                                                                
			
		</xsl:if>                                
		
		<xsl:text>&#xa;</xsl:text>
		<br/><xsl:text>&#xa;</xsl:text>
		<a class="txm-page" title="{count(preceding::tei:pb) + 2}"/>
		<span class="{$editionpagetype}"> - <xsl:value-of select="$pagenumber"/> - </span><br/><xsl:text>&#xa;</xsl:text>
	</xsl:template>
	
	<!-- Notes -->
	<xsl:template match="tei:note[not(@place='inline') and not(matches(@type,'intern|auto'))]">
		<!--<span style="color:violet"> [<b>Note :</b> <xsl:apply-templates/>] </span>-->	
		<xsl:variable name="note_count"><xsl:value-of select="count(preceding::tei:note[not(@place='inline') and not(matches(@type,'intern|auto'))]) + 1"/></xsl:variable>
		<a title="{.}" style="font-size:75%;position:relative;top:-5px" href="#note_{$note_count}" name="noteref_{$note_count}">[<xsl:value-of select="$note_count"/>]</a>
		<xsl:call-template name="spacing"/>                                
	</xsl:template>
	
                
                <xsl:template match="//tei:w"><span class="w">
                                                <xsl:if test="@id">
                                                	<xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute>
                                                </xsl:if>
                                	<xsl:attribute name="title">
                                		<xsl:if test="@id">
                                			<xsl:value-of select="@id"></xsl:value-of>
                                		</xsl:if>
                                		<xsl:if test="ancestor::tei:corr">
                                			<xsl:value-of select="concat(' sic : ',@sic)"/>
                                		</xsl:if>
                                		<xsl:if test="ancestor::tei:reg">
                                			<xsl:value-of select="concat(' orig : ',@orig)"/>
                                		</xsl:if>
                                		<xsl:choose>
                                			<xsl:when test="descendant::txm:ana">	
                                					<xsl:for-each select="descendant::txm:ana">
                                						<xsl:value-of select="concat(' ',substring-after(@type,'#'),' : ',.)"/>
                                					</xsl:for-each>
                                			</xsl:when>
                                			<xsl:otherwise>
                                				<xsl:for-each select="@*[not(local-name()='id')]">
                                					<xsl:value-of select="concat(' ',name(.),' : ',.)"/>
                                				</xsl:for-each>                                				
                                			</xsl:otherwise>
                                		</xsl:choose>
                                		<xsl:if test="@*[matches(name(.),'pos$')]">
                                		</xsl:if>                                		
                                	</xsl:attribute>
                	<xsl:choose>
                		<xsl:when test="descendant::txm:form">
                			<xsl:apply-templates select="txm:form"/>
                		</xsl:when>
                		<xsl:otherwise><xsl:apply-templates/></xsl:otherwise>
                	</xsl:choose>
                	
                                </span><xsl:call-template name="spacing"/></xsl:template>
                
<!--                <xsl:template match="//txm:form">
                                <xsl:apply-templates/>
                </xsl:template>
-->                
	<xsl:template name="spacing">
		<xsl:choose>
			<xsl:when test="$inputtype='xmltxm'">
				<xsl:call-template name="spacing-xmltxm"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="spacing-xmlw"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="spacing-xmlw">
		<xsl:choose>
			<xsl:when test="ancestor::tei:w"/>
			<xsl:when test="following::tei:w[1][matches(.,'^\s*[.,)\]]+\s*$')]"/>			
			<xsl:when test="matches(.,'^\s*[(\[]+$|\w(''|’)\s*$')"></xsl:when>
			<xsl:when test="position()=last() and (ancestor::tei:choice or ancestor::tei:supplied[not(@rend='multi_s')])"></xsl:when>
			<xsl:when test="following-sibling::*[1][self::tei:note]"></xsl:when>
			<xsl:when test="following::tei:w[1][matches(.,'^\s*[:;!?]+\s*$')]">
				<xsl:text>&#xa0;</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text> </xsl:text>
			</xsl:otherwise>
		</xsl:choose>                
	</xsl:template>

	<xsl:template name="spacing-xmltxm">
		<xsl:choose>
			<xsl:when test="ancestor::tei:w"/>
			<xsl:when test="following::tei:w[1][matches(descendant::txm:form[1],'^[.,)\]]+$')]"/>			
			<xsl:when test="matches(descendant::txm:form[1],'^[(\[]+$|\w(''|’)$')"></xsl:when>
			<xsl:when test="position()=last() and (ancestor::tei:choice or ancestor::tei:supplied[not(@rend='multi_s')])"></xsl:when>
			<xsl:when test="following-sibling::*[1][self::tei:note]"></xsl:when>
			<xsl:when test="following::tei:w[1][matches(descendant::txm:form[1],'^[:;!?]+$')]">
				<xsl:text>&#xa0;</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text> </xsl:text>
			</xsl:otherwise>
		</xsl:choose>                
	</xsl:template>

                
</xsl:stylesheet>
