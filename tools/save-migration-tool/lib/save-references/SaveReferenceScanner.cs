// Counts the content IDs a campaign.xml names, by kind, ID and site, in one streaming pass.
// A save runs to hundreds of megabytes and millions of elements. PowerShell's per-call cost makes a read loop take
// a minute or more, where this pass takes seconds, so the reading lives here and what to read stays in PowerShell:
// Get-SaveReferenceSites describes each site, and Read-SaveReferences hands the sites in.
using System;
using System.Collections.Generic;
using System.Text.Json;
using System.Xml;

namespace KMLib.SaveMigrationTool
{
    // One place a save names content by ID, as New-SaveReferenceSite describes it.
    public sealed class SaveReferenceSite
    {
        public string Kind;
        public string Name;

        // Element names from the outermost to the one holding the ID. Each matches an element's own name or the
        // class its cl attribute names, as XStream writes a field's runtime class; * matches any element.
        public string[] Path;

        // The attribute of the last element holding the ID; null when the element's text holds it.
        public string Attribute;

        // The first element of the path must carry this attribute with one of these values.
        public string FilterAttribute;
        public string[] FilterValues;

        // The last element's place among its parent's children of the same name, from 1; 0 for any.
        public int Position;

        // The text is a JSON object: its keys are the IDs, or the value under this key is the ID.
        public bool AreJsonKeysIds;
        public string JsonKey;
    }

    // What one scan found: each kind's IDs with their uses per site, and the values no site could read.
    public sealed class SaveReferenceScan
    {
        public Dictionary<string, Dictionary<string, Dictionary<string, int>>> UsesByKind =
            new(StringComparer.Ordinal);

        public Dictionary<string, int> UnreadableCountBySite = new(StringComparer.Ordinal);

        public long ElementCount;
    }

    public static class SaveReferenceScanner
    {
        private const string AnyElement = "*";
        private const string ClassAttribute = "cl";

        private sealed class Frame
        {
            public string Name;
            public string ClassName;
            public Dictionary<string, string> FilterValues;
            public Dictionary<string, int> ChildCountByName;
            public int Position;
            public List<SaveReferenceSite> TextSites;
        }

        public static SaveReferenceScan Scan(string path, SaveReferenceSite[] sites)
        {
            var sitesByLastName = new Dictionary<string, List<SaveReferenceSite>>(StringComparer.Ordinal);
            var filterAttributes = new HashSet<string>(StringComparer.Ordinal);

            foreach (var site in sites)
            {
                var lastName = site.Path[^1];

                if (!sitesByLastName.TryGetValue(lastName, out var named))
                {
                    named = [];
                    sitesByLastName.Add(lastName, named);
                }

                named.Add(site);

                if (site.FilterAttribute != null)
                {
                    filterAttributes.Add(site.FilterAttribute);
                }
            }

            // XStream writes characters XML 1.0 forbids, such as control characters in a ship's name.
            var settings = new XmlReaderSettings
            {
                DtdProcessing = DtdProcessing.Prohibit,
                XmlResolver = null,
                CheckCharacters = false,
                IgnoreComments = true,
                IgnoreWhitespace = true,
            };

            var scan = new SaveReferenceScan();
            var stack = new List<Frame>();

            using (var reader = XmlReader.Create(path, settings))
            {
                while (reader.Read())
                {
                    switch (reader.NodeType)
                    {
                        case XmlNodeType.Element:
                            scan.ElementCount++;
                            var frame = PushFrame(reader, stack, filterAttributes);
                            MatchSites(reader, stack, frame, sitesByLastName, scan);

                            if (reader.IsEmptyElement)
                            {
                                stack.RemoveAt(stack.Count - 1);
                            }

                            break;

                        case XmlNodeType.EndElement:
                            stack.RemoveAt(stack.Count - 1);
                            break;

                        case XmlNodeType.Text:
                        case XmlNodeType.CDATA:
                            var top = stack[^1];

                            if (top.TextSites != null)
                            {
                                foreach (var site in top.TextSites)
                                {
                                    CountValue(scan, site, reader.Value);
                                }
                            }

                            break;
                    }
                }
            }

            return scan;
        }

        private static Frame PushFrame(
            XmlReader reader,
            List<Frame> stack,
            HashSet<string> filterAttributes)
        {
            var frame = new Frame {
                Name = reader.LocalName,
                ClassName = reader.GetAttribute(ClassAttribute) };

            foreach (var attribute in filterAttributes)
            {
                var value = reader.GetAttribute(attribute);
                if (value != null)
                {
                    frame.FilterValues ??= new Dictionary<string, string>(StringComparer.Ordinal);
                    frame.FilterValues.Add(attribute, value);
                }
            }

            if (stack.Count > 0)
            {
                var parent = stack[^1];

                parent.ChildCountByName ??= new Dictionary<string, int>(StringComparer.Ordinal);
                parent.ChildCountByName.TryGetValue(frame.Name, out var count);

                frame.Position = count + 1;

                parent.ChildCountByName[frame.Name] = frame.Position;
            }

            stack.Add(frame);

            return frame;
        }

        private static void MatchSites(
            XmlReader reader,
            List<Frame> stack,
            Frame frame,
            Dictionary<string, List<SaveReferenceSite>> sitesByLastName,
            SaveReferenceScan scan)
        {
            foreach (var candidateName in new[] { frame.Name, frame.ClassName, AnyElement })
            {
                if (candidateName == null
                    || !sitesByLastName.TryGetValue(candidateName, out var candidates))
                {
                    continue;
                }

                foreach (var site in candidates)
                {
                    if (!IsPathMatched(stack, site))
                    {
                        continue;
                    }

                    if (site.Attribute == null)
                    {
                        frame.TextSites ??= new List<SaveReferenceSite>();
                        frame.TextSites.Add(site);
                        continue;
                    }

                    var value = reader.GetAttribute(site.Attribute);
                    if (value != null)
                    {
                        CountValue(scan, site, value);
                    }
                }
            }
        }

        private static bool IsPathMatched(List<Frame> stack, SaveReferenceSite site)
        {
            var offset = stack.Count - site.Path.Length;
            if (offset < 0)
            {
                return false;
            }

            for (var index = 0; index < site.Path.Length; index++)
            {
                var frame = stack[offset + index];
                var name = site.Path[index];
                var isNameMatched = name == AnyElement
                    || name == frame.Name
                    || name == frame.ClassName;

                if (!isNameMatched)
                {
                    return false;
                }
            }

            if (site.Position > 0
                && stack[^ 1].Position != site.Position)
            {
                return false;
            }

            if (site.FilterAttribute == null)
            {
                return true;
            }

            var first = stack[offset];
            string filterValue = null;

            var hasFilterValue = first.FilterValues != null
                && first.FilterValues.TryGetValue(site.FilterAttribute, out filterValue);

            return hasFilterValue
                && Array.IndexOf(site.FilterValues, filterValue) >= 0;
        }

        private static void CountValue(SaveReferenceScan scan, SaveReferenceSite site, string value)
        {
            if (!site.AreJsonKeysIds
                && site.JsonKey == null)
            {
                CountId(scan, site, value);
                return;
            }

            try
            {
                using var document = JsonDocument.Parse(value);

                if (document.RootElement.ValueKind != JsonValueKind.Object)
                {
                    CountUnreadable(scan, site);
                    return;
                }

                if (site.AreJsonKeysIds)
                {
                    foreach (var property in document.RootElement.EnumerateObject())
                    {
                        CountId(scan, site, property.Name);
                    }

                    return;
                }

                if (document.RootElement.TryGetProperty(site.JsonKey, out var keyed) &&
                    keyed.ValueKind == JsonValueKind.String)
                {
                    CountId(scan, site, keyed.GetString());

                }
            }
            catch (JsonException)
            {
                CountUnreadable(scan, site);
            }
        }

        private static void CountId(SaveReferenceScan scan, SaveReferenceSite site, string id)
        {
            if (id.Length == 0)
            {
                return;
            }

            if (!scan.UsesByKind.TryGetValue(site.Kind, out var usesById))
            {
                usesById = new Dictionary<string, Dictionary<string, int>>(StringComparer.Ordinal);
                scan.UsesByKind.Add(site.Kind, usesById);
            }

            if (!usesById.TryGetValue(id, out var usesBySite))
            {
                usesBySite = new Dictionary<string, int>(StringComparer.Ordinal);
                usesById.Add(id, usesBySite);
            }

            usesBySite.TryGetValue(site.Name, out var count);
            usesBySite[site.Name] = count + 1;
        }

        private static void CountUnreadable(SaveReferenceScan scan, SaveReferenceSite site)
        {
            scan.UnreadableCountBySite.TryGetValue(site.Name, out var count);
            scan.UnreadableCountBySite[site.Name] = count + 1;
        }
    }
}
