<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">
	  <xsl:variable name="fo:layout-master-set">
        <fo:layout-master-set>
              <fo:simple-page-master master-name="cover-page-master" page-height="11in" page-width="8.5in">
                <fo:region-body />
            </fo:simple-page-master>
            <fo:simple-page-master master-name="default-page" page-height="11in" page-width="8.5in" margin-left="0.6in" margin-right="0.6in">
                <fo:region-before extent="0.79in" />
                <fo:region-body margin-top="0.79in" margin-bottom="0.79in" />
            </fo:simple-page-master>
        </fo:layout-master-set>
    </xsl:variable>
    <xsl:template match="/">
        <fo:root>
            <xsl:copy-of select="$fo:layout-master-set" />
             <fo:page-sequence master-reference="cover-page-master">
                <fo:flow flow-name="xsl-region-body">
                    <fo:block>
                        <fo:table start-indent="(8.5in - ((8.5in * 80) div 100) ) div 2" end-indent="(8.5in - ((8.5in * 80) div 100) ) div 2" text-align="center" width="80%" space-before.optimum="1pt" space-after.optimum="2pt">
                            <fo:table-column column-width="20pt" />
                            <fo:table-column column-width="311pt" />
                            <fo:table-column column-width="222pt" />
                            <fo:table-body>
                                <fo:table-row>
                                    <fo:table-cell height="56pt" width="20pt" padding-start="3pt" padding-end="3pt" padding-before="3pt" padding-after="3pt" display-align="center" text-align="start" border-style="solid" border-width="1pt" border-color="white">
                                        <fo:block />
                                    </fo:table-cell>
                                    <fo:table-cell height="56pt" text-align="center" width="311pt" padding-start="3pt" padding-end="3pt" padding-before="3pt" padding-after="3pt" display-align="center" border-style="solid" border-width="1pt" border-color="white">
                                        <fo:block />
                                    </fo:table-cell>
                                    <fo:table-cell height="56pt" text-align="center" width="222pt" padding-start="3pt" padding-end="3pt" padding-before="3pt" padding-after="3pt" display-align="center" border-style="solid" border-width="1pt" border-color="white">
                                        <fo:block />
                                    </fo:table-cell>
                                </fo:table-row>
                                <fo:table-row>
                                    <fo:table-cell height="229pt" number-rows-spanned="2" width="20pt" padding-start="3pt" padding-end="3pt" padding-before="3pt" padding-after="3pt" display-align="center" text-align="start" border-style="solid" border-width="1pt" border-color="white">
                                        <fo:block />
                                    </fo:table-cell>
                                    <fo:table-cell height="229pt" text-align="center" width="311pt" padding-start="3pt" padding-end="3pt" padding-before="3pt" padding-after="3pt" display-align="center" border-style="solid" border-width="1pt" border-color="white">
                                        <fo:block>
                                            <fo:external-graphic src="xdocs/stylesheets/fop/logo.jpg" content-height="181px" content-width="308px" space-before.optimum="4pt" space-after.optimum="4pt"/>
                                        </fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell height="229pt" text-align="center" width="222pt" padding-start="3pt" padding-end="3pt" padding-before="3pt" padding-after="3pt" display-align="center" border-style="solid" border-width="1pt" border-color="white">
                                        <fo:block>
                                            <fo:block>
                                                <fo:leader leader-pattern="space" />
                                            </fo:block>
                                            <fo:block>
                                                <fo:leader leader-pattern="space" />
                                            </fo:block>
                                            <fo:block>
                                                <fo:leader leader-pattern="space" />
                                            </fo:block>
                                             <fo:inline color="black" font-size="20pt">
                                                  <xsl:text>Cayenne</xsl:text>
                                            </fo:inline>
                                            <fo:block>
                                                <fo:leader leader-pattern="space" />
                                            </fo:block>
                                            <fo:inline color="black" font-size="20pt">Framework</fo:inline>
                                        </fo:block>
                                    </fo:table-cell>
                                </fo:table-row>
                                <fo:table-row>
                                    <fo:table-cell height="551pt" number-columns-spanned="2" text-align="left" width="542pt" padding-start="3pt" padding-end="3pt" padding-before="3pt" padding-after="3pt" display-align="center" border-style="solid" border-width="1pt" border-color="white">
                                        <fo:block>
                                            <fo:block>
                                                <fo:leader leader-pattern="space" />
                                            </fo:block>
                                            <fo:block>
                                                <fo:leader leader-pattern="space" />
                                            </fo:block>
                                            <fo:block>
                                                <fo:leader leader-pattern="space" />
                                            </fo:block>
                                            <fo:block>
                                                <fo:leader leader-pattern="space" />
                                            </fo:block>
                                            <fo:block>
                                                <fo:leader leader-pattern="space" />
                                            </fo:block>
                                            <fo:block>
                                                <fo:leader leader-pattern="space" />
                                            </fo:block>
                                            <fo:block text-align="center">
                                                     <fo:inline font-size="30pt">&#160;&#160;<xsl:value-of select="/document/@title"/>
 </fo:inline>
                                            </fo:block>         
                                            <fo:block>
                                                <fo:leader leader-pattern="space" />
                                            </fo:block>
                                            <fo:block>
                                                <fo:leader leader-pattern="space" />
                                            </fo:block>
                                            <fo:block>
                                                <fo:leader leader-pattern="space" />
                                            </fo:block>
                                            <fo:block>
                                                <fo:leader leader-pattern="space" />
                                            </fo:block>
                                            <fo:block>
                                                <fo:leader leader-pattern="space" />
                                            </fo:block>
                                            <fo:block>
                                                <fo:leader leader-pattern="space" />
                                            </fo:block>
                                            <fo:block>
                                                <fo:leader leader-pattern="space" />
                                            </fo:block>
                                            <fo:block>
                                                <fo:leader leader-pattern="space" />
                                            </fo:block>
                                            <fo:block>
                                                <fo:leader leader-pattern="space" />
                                            </fo:block>
                                            <fo:block>
                                                <fo:leader leader-pattern="space" />
                                            </fo:block>
                                            <fo:block>
                                                <fo:leader leader-pattern="space" />
                                            </fo:block>
                                            <fo:block>
                                                <fo:leader leader-pattern="space" />
                                            </fo:block>
                                            <fo:block>
                                                <fo:leader leader-pattern="space" />
                                            </fo:block>
                                            <fo:block>
                                                <fo:leader leader-pattern="space" />
                                            </fo:block>
                                            <fo:block>
                                                <fo:leader leader-pattern="space" />
                                            </fo:block>
                                            <fo:block>
                                                <fo:leader leader-pattern="space" />
                                            </fo:block>
                                            <fo:block text-align="center">
                                                <fo:inline color="black" font-size="20pt">
                                                  <xsl:text>2005</xsl:text>
                                               </fo:inline>
                                            </fo:block>
                                        </fo:block>
                                    </fo:table-cell>
                                </fo:table-row>
                            </fo:table-body>
                        </fo:table>
                    </fo:block>
                </fo:flow>
            </fo:page-sequence> 
            <fo:page-sequence master-reference="default-page" initial-page-number="1" format="1">
                <fo:static-content flow-name="xsl-region-before">
                    <fo:block>
                        <fo:table width="100%" space-before.optimum="1pt" space-after.optimum="2pt">
                            <fo:table-column />
                            <fo:table-column column-width="150pt" />
                            <fo:table-body>
                                <fo:table-row>
                                    <fo:table-cell padding-after="0pt" padding-before="0pt" padding-end="0pt" padding-start="0pt" height="30pt" number-columns-spanned="2" display-align="center" text-align="start" border-style="solid" border-width="1pt" border-color="white">
                                        <fo:block />
                                    </fo:table-cell>
                                </fo:table-row>
                                <fo:table-row>
                                    <fo:table-cell font-size="inherited-property-value(&apos;font-size&apos;) - 2pt" padding-after="0pt" padding-before="0pt" padding-end="0pt" padding-start="0pt" text-align="left" display-align="center" border-style="solid" border-width="1pt" border-color="white">
                                        <fo:block>
                                            <fo:inline font-weight="bold">Title: Cayenne documentation</fo:inline>
                                        </fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell font-size="inherited-property-value(&apos;font-size&apos;) - 2pt" padding-after="0pt" padding-before="0pt" padding-end="0pt" padding-start="0pt" text-align="right" width="150pt" display-align="center" border-style="solid" border-width="1pt" border-color="white">
                                        <fo:block>
                                            <fo:inline font-weight="bold">Page: </fo:inline>
                                            <fo:page-number font-weight="bold" />
                                        </fo:block>
                                    </fo:table-cell>
                                </fo:table-row>
                                <fo:table-row>
                                    <fo:table-cell padding-after="0pt" padding-before="0pt" padding-end="0pt" padding-start="0pt" number-columns-spanned="2" display-align="center" text-align="start" border-style="solid" border-width="1pt" border-color="white">
                                        <fo:block>
                                            <fo:block color="black" space-before.optimum="-8pt">
                                                <fo:leader leader-length="100%" leader-pattern="rule" rule-thickness="1pt" />
                                            </fo:block>
                                        </fo:block>
                                    </fo:table-cell>
                                </fo:table-row>
                            </fo:table-body>
                        </fo:table>
                    </fo:block>
                </fo:static-content>
                <fo:flow flow-name="xsl-region-body">
                    <xsl:for-each select="document">
						<xsl:for-each select="body">
							<xsl:for-each select="section">
								<xsl:for-each select="@name">
									<fo:block>
										<fo:leader leader-pattern="space"/>
									</fo:block>
									<fo:block>
										<fo:leader leader-pattern="space"/>
									</fo:block>
									<fo:block>
										<xsl:text>&#xA;</xsl:text>
									</fo:block>
									<fo:block font-size="20pt" font-weight="bold" space-before.optimum="1pt" space-after.optimum="2pt">
										<fo:block text-align="left">
											<fo:inline color="black" font-weight="bold">
												<xsl:value-of select="."/>
											</fo:inline>
										</fo:block>
									</fo:block>
									<fo:block>
										<fo:leader leader-pattern="space"/>
									</fo:block>
								</xsl:for-each>
								<xsl:for-each select="p | ul | b | i | table | strong | source | ul | panel | img ">
									<xsl:apply-templates select="."/>
								</xsl:for-each>
								<xsl:apply-templates select="./subsection"/>
							</xsl:for-each>
						</xsl:for-each>
					</xsl:for-each>                   
                </fo:flow>
            </fo:page-sequence>
        </fo:root>
    </xsl:template>		
		
	
	<xsl:template match="subsection">
		<xsl:for-each select="@name">
			<fo:block>
				<fo:leader leader-pattern="space"/>
			</fo:block>
			<fo:block>
				<xsl:text>&#xA;</xsl:text>
			</fo:block>
			<fo:block font-size="16pt" font-weight="bold" space-before.optimum="1pt" space-after.optimum="2pt">
				<fo:block text-align="left" color="black" font-weight="bold">
					<xsl:value-of select="."/>
				</fo:block>
			</fo:block>
		</xsl:for-each>
		<xsl:for-each select="p | ul | b | i | table | strong | source | ul | panel | img | subsection">
			<xsl:apply-templates select="."/>
		</xsl:for-each>
	</xsl:template>
	<xsl:template match="a">
		             <fo:basic-link text-decoration="underline" color="blue">
			                  <xsl:attribute name="external-destination"><xsl:value-of select="@href"/></xsl:attribute>
			                 <fo:inline>
				                 <xsl:apply-templates/>
			                 </fo:inline>
		            </fo:basic-link>	
	</xsl:template>
	<xsl:template match="b">
		<fo:inline font-weight="bold">
			<xsl:apply-templates/>
		</fo:inline>
	</xsl:template>
	<xsl:template match="code">
		<xsl:apply-templates/>
	</xsl:template>
	<xsl:template match="i">
		<fo:inline font-style="italic">
			<xsl:apply-templates/>
		</fo:inline>
	</xsl:template>
	<xsl:template match="img">
	     <fo:block text-align="center">
			<fo:external-graphic src="{@src}" content-type="content-type:image/gif" scaling="uniform" border-style="solid" border-color="black"/>
	    </fo:block>
	</xsl:template>

	<xsl:template match="p">
        <fo:block space-before.optimum="1pt" space-after.optimum="2pt">
            <fo:block>
                <fo:inline font-size="#pt10">
                    <xsl:apply-templates />
                </fo:inline>
            </fo:block>
        </fo:block>
    </xsl:template>

	<xsl:template match="panel">
		<fo:block space-before.optimum="1pt" space-after.optimum="2pt" vertical-align="middle">
			<fo:block background-color="#CCCCCC" border-top-style="solid" border-top-color="black" border-top-width="0.5mm" border-start-style="solid" border-start-color="black" border-start-width="0.5mm" border-end-color="black" border-end-style="solid" border-end-width="0.5mm" space-before.optimum="1mm" font-weight="bold" vertical-align="middle">
				<fo:external-graphic src="{@icon}" content-type="content-type:image/gif" vertical-align="middle" scaling="uniform" width="16px" height="16px"/>
				<xsl:text>&#xA;</xsl:text>
				<xsl:value-of select="@name"/>
			</fo:block>
			<fo:block background-color="#CCCCCC" font-size="#pt11" font-family="Times" border-start-color="black" border-start-width="0.5mm" border-start-style="solid" border-end-color="black" border-end-style="solid" border-end-width="0.5mm" border-bottom-color="black" border-bottom-style="solid" border-bottom-width="0.5mm">
				<xsl:value-of select="."/>
			</fo:block>
		</fo:block>
	</xsl:template>
	<xsl:template match="source">
		<fo:block>
			<xsl:text>&#xA;</xsl:text>
		</fo:block>
		<fo:block space-before.optimum="1pt" space-after.optimum="2pt" font-family="Courier" font-size="9pt" background-color="#FFFFCC" border-style="solid" border-color="black">
			<fo:block text-align="left">&#160;&#160; <xsl:apply-templates/>
			</fo:block>
		</fo:block>
		<fo:block>
			<xsl:text>&#xA;</xsl:text>
		</fo:block>
		<fo:block>
			<fo:leader leader-pattern="space"/>
		</fo:block>
	</xsl:template>
	<xsl:template match="strong">
		<fo:inline font-weight="bold">
			<xsl:apply-templates/>
		</fo:inline>
	</xsl:template>
	<xsl:template match="table">
		<fo:table width="100%">
			<fo:table-header/>
			<xsl:for-each select="./tr/th">
				<fo:table-column/>
			</xsl:for-each>
			<fo:table-body>
				<xsl:for-each select="tr">
					<fo:table-row>
						<xsl:for-each select="th | td">
							<xsl:apply-templates select="."/>
						</xsl:for-each>
					</fo:table-row>
				</xsl:for-each>
			</fo:table-body>
		</fo:table>
	</xsl:template>
	<xsl:template match="th">
		<fo:table-cell display-align="center" border-style="solid" border-width="1pt" border-color="#999966" background-color="#FFFF99">
			<fo:block text-align="center">
				<xsl:value-of select="."/>
			</fo:block>
		</fo:table-cell>
	</xsl:template>
	<xsl:template match="td">
		<fo:table-cell display-align="center" border-style="solid" border-width="1pt" border-color="#999966" background-color="#FFFFCC">
			<fo:block text-align="center">
				<xsl:value-of select="."/>
			</fo:block>
		</fo:table-cell>
	</xsl:template>
	<xsl:template match="ul">
		<fo:list-block provisional-distance-between-starts="4mm" provisional-label-separation="4mm" start-indent="15mm" space-before.optimum="4pt" space-after.optimum="4pt">
			<xsl:for-each select="li">
				<fo:list-item text-align="left">
					<fo:list-item-label end-indent="label-end()" text-align="left">
						<fo:block font-family="Courier" font-size="15pt" line-height="14pt" padding-before="2pt">&#x2022;</fo:block>
					</fo:list-item-label>
					<fo:list-item-body start-indent="body-start()">
						<fo:block>
							<xsl:apply-templates/>
						</fo:block>
					</fo:list-item-body>
				</fo:list-item>
			</xsl:for-each>
		</fo:list-block>
		<fo:block>
			<fo:leader leader-pattern="space"/>
		</fo:block>
	</xsl:template>
</xsl:stylesheet>
