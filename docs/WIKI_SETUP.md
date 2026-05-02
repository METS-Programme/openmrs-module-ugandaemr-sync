# UgandaEMR Sync Module Wiki

This directory contains the GitHub Wiki for the UgandaEMR Sync Module.

## Wiki Pages

### Essential Pages
- **[Home](Home.md)** - Module overview and quick links
- **[Getting Started](Getting-Started.md)** - 15-minute installation guide
- **[Architecture](Architecture.md)** - System architecture and design
- **[Configuration](Configuration.md)** - Configuration options and settings
- **[API Reference](API-Reference.md)** - Complete REST API documentation

### Specialized Guides
- **[FHIR Profiles](FHIR-Profiles.md)** - FHIR profile configuration guide
- **[Development](Development.md)** - Developer guide and contribution
- **[Troubleshooting](Troubleshooting.md)** - Common issues and solutions
- **[Security](Security.md)** - Security considerations and best practices

## Setting Up the Wiki

### Option 1: Automatic Wiki Setup (Recommended)

1. Create a `gh-pages` branch or use GitHub's built-in wiki
2. Copy all `.md` files from this directory to the wiki
3. Organize pages with proper sidebar navigation

### Option 2: Manual Wiki Setup

1. Go to your GitHub repository
2. Click on the "Wiki" tab
3. Click "Create the first page" or "Add Page"
4. Copy the contents from each `.md` file
5. Create pages with the same names (without `.md` extension)
6. Set up the sidebar navigation

### Option 3: Using GitHub Wiki CLI

```bash
# Install gowiki (if needed)
npm install -g gowiki

# Clone the wiki
git clone https://github.com/METS-Programme/openmrs-module-ugandaemr-sync.wiki.git

# Copy files to wiki directory
cp wiki/*.md openmrs-module-ugandaemr-sync.wiki/

# Commit and push
cd openmrs-module-ugandaemr-sync.wiki
git add .
git commit -m "Add comprehensive wiki documentation"
git push origin master
```

## Sidebar Configuration

Create a `_Sidebar.md` file in your wiki with:

```markdown
# UgandaEMR Sync Module Wiki

## Getting Started
- [Home](Home)
- [Getting Started](Getting-Started)
- [Architecture](Architecture)

## Documentation
- [Configuration](Configuration)
- [API Reference](API-Reference)
- [FHIR Profiles](FHIR-Profiles)

## Development
- [Development Guide](Development)
- [Troubleshooting](Troubleshooting)
- [Security](Security)

## External Links
- [GitHub Repository](https://github.com/METS-Programme/openmrs-module-ugandaemr-sync)
- [Issue Tracker](https://github.com/METS-Programme/openmrs-module-ugandaemr-sync/issues)
- [OpenMRS Talk](https://talk.openmrs.org/)
```

## Footer Configuration

Create a `_Footer.md` file in your wiki with:

```markdown
---
**UgandaEMR Sync Module** v2.0.6-SNAPSHOT  
[© 2026 METS Programme](http://mets.or.ug)  
[Licensed under MPL 2.0](https://opensource.org/licenses/MPL-2.0)

Need help? [Open an issue](https://github.com/METS-Programme/openmrs-module-ugandaemr-sync/issues) or visit [OpenMRS Talk](https://talk.openmrs.org/)
```

## Wiki Structure

```
wiki/
├── Home.md                    # Main landing page
├── Getting-Started.md          # Quick start guide
├── Architecture.md             # System architecture
├── Configuration.md            # Configuration guide
├── API-Reference.md            # REST API documentation
├── FHIR-Profiles.md            # FHIR profile guide
├── Development.md              # Developer guide
├── Troubleshooting.md          # Troubleshooting guide
├── Security.md                 # Security documentation
├── README.md                   # This file
├── _Sidebar.md                 # Sidebar navigation (create in wiki)
└── _Footer.md                  # Footer content (create in wiki)
```

## Maintenance

### Updating the Wiki

1. Update the relevant `.md` files in this directory
2. Test changes locally
3. Commit changes to the repository
4. Update the GitHub wiki if using separate wiki repo

### Versioning

- Wiki documentation should match the module version
- Update version numbers in each page header
- Add changelog entries for major documentation changes

### Quality Checks

- Ensure all links work correctly
- Verify code examples are accurate
- Check for spelling and grammar errors
- Maintain consistent formatting

## Best Practices

1. **Keep It Simple**: Use clear, concise language
2. **Be Specific**: Provide exact commands and examples
3. **Stay Current**: Update documentation with each release
4. **Cross-Reference**: Link between related pages
5. **Use Examples**: Provide practical examples for complex topics
6. **Visual Aids**: Use diagrams for complex concepts
7. **Troubleshooting**: Include common issues and solutions

## Contributing

We welcome documentation improvements! Please:

1. Fork the repository
2. Create a documentation branch
3. Make your improvements
4. Submit a pull request

## Support

For documentation issues or questions:
- **GitHub Issues**: [Report documentation problems](https://github.com/METS-Programme/openmrs-module-ugandaemr-sync/issues)
- **OpenMRS Talk**: [Community discussions](https://talk.openmrs.org/)
- **METS Programme**: [Professional support](http://mets.or.ug)

---

**Wiki Version**: 1.0  
**Last Updated**: May 2, 2026  
**Maintained By**: METS Programme Documentation Team