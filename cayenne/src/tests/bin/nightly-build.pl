#!/usr/bin/perl

#
# Cayenne automated build script. Performs checkout of Cayenne via anonymous CVS,
# compiles and runs unit tests. Can optionaly notify about the outcome via email
# as well as upload successful builds to the download server. Used primarily for
# nightly builds. Requires:
# 
#   1. A UNIX box with Perl 
#   2. Ant 1.5
#   3. JDK 1.4
#   4. cvs
#   5. qmail
#   6. Entry in $HOME/.cayenne/connection.properties for "nightly-test"
#
# Command line:
#     nightly-build.pl [-u] [-m email@example.com] 
#
# Crontab:
#
#     2 5 * * * /fullpathto/nightly-build.pl [-u] [-m email@example.com]  2>&1 > /dev/null
#

use strict;
use File::Path;
use File::Copy;
use Getopt::Std;
use Cwd;


# These must be defined as environment variables
# (May need to modify script to make this configurable on the command line)
$ENV{'JAVA_HOME'} = "/opt/java";
$ENV{'ANT_HOME'} = "/opt/ant";

our ($opt_u, $opt_m);
getopts('um:');

my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst) = localtime();
$year = 1900 + $year;
$mon = 1 + $mon;	


die_with_email("No JDK1.4 installation at $ENV{'JAVA_HOME'}") unless -d $ENV{'JAVA_HOME'};

my $cayenne_src = "/tmp/cayenne/build";
my $ant = "$ENV{'ANT_HOME'}/bin/ant";
die_with_email("No Ant installation at $ant") unless -f $ant;

# Upload path on the server
my $rel_path = "/var/www/objectstyle/downloads/cayenne/nightly";

# print timestamp for log
print "\n\n===================================================\n";
print "Nightly build: $mon-$mday-$year\n";

# Prepare checkout directory
rmtree($cayenne_src, 0, 1) if -d $cayenne_src;
mkpath($cayenne_src, 1, 0711) or die_with_email("Can't create build directory: $!\n");
chdir $cayenne_src or die_with_email("Can't change to $cayenne_src: $!\n");

# checkout via anonymous CVS
# assume anonymous password is already in ~/.cvspass
my $status = system(
	"cvs", 
	"-z3",
	"-q",
	"-d:pserver:anonymous\@cvs.cayenne.sourceforge.net:/cvsroot/cayenne",
	"export",
	"-D",
	"1 minute ago",
	"cayenne");
die_with_email("CVS checkout failed, return status: $status\n") if $status;

# build
chdir "$cayenne_src/cayenne" or die_with_email("Can't change to $cayenne_src/cayenne: $!\n");
$status = system($ant, "release");
die_with_email("Build failed, return status: $status\n") if $status;


# unit tests - ant
$status = system($ant, "test", "-Dcayenne.test.connection=nightly-test");
die_with_email("Unit tests failed, return status: $status\n") if $status;


# upload
if($opt_u) {
	# make remote upload directory	
	$status = system("ssh", "www.objectstyle.org", "mkdir", "-p", "$rel_path/$year-$mon-$mday");
	die_with_email("Can't create release directory, return status: $status\n") if $status;
	
	my @gz_files = <dist/*.gz>;
	die "Distribution file not found." unless @gz_files;
	
	$status = system("scp", $gz_files[0], "www.objectstyle.org:$rel_path/$year-$mon-$mday/");
	die_with_email("Can't upload release, return status: $status\n") if $status;
	success_email("Build Succeeded.");
}

sub success_email() {
	if($opt_m) {
		my $msg = $_[0];
    
		open(MAIL, "| mail -s "Cayenne Build Succeeded ($mon/$mday/$year)" $opt_m") 
			or die  "Can't send mail: $!\n";
    
		print MAIL "\n";
		print MAIL "Message:\n\n";
		print MAIL "  $msg\n";
		close(MAIL);   
	}
}


sub die_with_email() {
	my $msg = $_[0];
	
	if($opt_m) {
    
		open(MAIL, "| mail -s "Subject: Cayenne Build Failed ($mon/$mday/$year)" $opt_m") 
		or die  "Can't send mail: $!\n";
    
		print MAIL "\n";
		print MAIL "Error message:\n\n";
		print MAIL "  $msg\n";
		close(MAIL);
	}
	
	die $msg;
}
