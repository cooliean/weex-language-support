package com.taobao.weex.lint;

import com.google.gson.Gson;
import com.taobao.weex.custom.Settings;
import com.taobao.weex.utils.ExtraModulesUtil;
import com.taobao.weex.utils.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Created by moxun on 16/10/12.
 */
public class DirectiveLint {

    private static WeexTag[] tags = null;
    private static Set<String> tagNames = null;
    private static String[] htmlTags = {
            "div",
            "text",
            "video",
            "a"
    };

    public static void dump() {
        if (tags != null) {
            List list = Arrays.asList(tags);
            Logger.warn("Tags: (" + list.size() + ") " + list.toString());
        } else {
            Logger.warn("Tags: null");
        }
        if (tagNames != null) {
            Logger.warn("Tag names: (" + tagNames.size() + ") " + tagNames.toString());
        } else {
            Logger.warn("Tag names: null");
        }
        List list1 = ExtraModulesUtil.getTagsFromNodeModules();
        Logger.warn("Tag from node_modules: (" + list1.size() + ") " + list1.toString());
    }

    public static void reset() {
        loadBuiltInRules();
        if (tagNames != null) {
            tagNames.clear();
        }
        mergeToBuiltIn(Settings.getRules());
        Logger.info("Rebuild weex component caches, " + tags.length + " tags found.");
    }

    public static void prepare() {
        if (tags == null) {
            loadBuiltInRules();
        }
        mergeToBuiltIn(Settings.getRules());
    }

    private static void mergeToTags(List<WeexTag> from) {
        List<WeexTag> builtIn = new ArrayList<WeexTag>(Arrays.asList(tags));
        if (from != null) {
            for (WeexTag tag : from) {
                if (containsTag(tag.tag)) {
                    WeexTag builtInTag = getBuiltInWeexTag(tag.tag);
                    if (builtInTag != null) {
                        mergeCustom(builtInTag, tag);
                    }
                } else {
                    builtIn.add(tag);
                }
            }
        }
        tags = builtIn.toArray(new WeexTag[builtIn.size()]);
        if (tagNames != null) {
            tagNames.clear();
        }
    }

    private static void loadBuiltInRules() {
        InputStream is = DirectiveLint.class.getResourceAsStream("/directives/directives.json");
        Gson gson = new Gson();
        tags = gson.fromJson(new InputStreamReader(is), WeexTag[].class);
        if (tags == null) {
            tags = new WeexTag[0];
        }
        loadNodeModules();
    }

    private static void loadNodeModules() {
        List<WeexTag> weexTags = ExtraModulesUtil.getTagsFromNodeModules();
        mergeToTags(weexTags);
        Logger.debug(weexTags.toString());
    }

    private static void mergeToBuiltIn(WeexTag[] customRules) {
        if (customRules != null) {
            mergeToTags(Arrays.asList(customRules));
        }
    }

    private static void mergeCustom(WeexTag builtIn, WeexTag custom) {
        if (custom.parent != null && custom.parent.size() > 0) {
            if (builtIn.parent != null) {
                builtIn.parent.addAll(custom.parent);
            } else {
                builtIn.parent = custom.parent;
            }
        }

        if (custom.child != null && custom.child.size() > 0) {
            if (builtIn.child != null) {
                builtIn.child.addAll(custom.child);
            } else {
                builtIn.child = custom.child;
            }
        }

        if (custom.attrs != null && custom.attrs.size() > 0) {
            if (builtIn.attrs != null) {
                for (Attribute attr : builtIn.attrs) {
                    if (custom.getAttribute(attr.name) != null) {
                        attr.merge(custom.getAttribute(attr.name));
                    }
                }
            }
            for (Attribute attr1 : custom.attrs) {
                if (builtIn.getAttribute(attr1.name) == null) {
                    builtIn.attrs.add(attr1);
                }
            }
        }
    }

    public static Set<String> getWeexTagNames() {
        if (tagNames == null) {
            tagNames = new HashSet<String>();
        }

        if (tagNames.size() == 0) {
            if (tags == null) {
                prepare();
            }
            for (WeexTag tag : tags) {
                if (!"common".equals(tag.tag)) {
                    tagNames.add(tag.tag);
                }
            }
        }

        Logger.debug(tagNames.toString());
        Logger.debug(Arrays.asList(tags).toString());

        return tagNames;
    }

    public static List<String> getHtmlTags() {
        return Arrays.asList(htmlTags);
    }

    private static boolean containsTag(String tagName) {
        for (WeexTag tag : tags) {
            if (tagName.equals(tag.tag)) {
                return true;
            }
        }
        return false;
    }

    private static WeexTag getBuiltInWeexTag(String tagName) {
        if (tags == null) {
            loadBuiltInRules();
        }
        for (WeexTag tag : tags) {
            if (tag.tag.equals(tagName)) {
                return tag;
            }
        }
        return null;
    }

    public static WeexTag getCommonTag() {
        if (tags == null) {
            prepare();
        }
        for (WeexTag tag : tags) {
            if (tag.tag.equals("common")) {
                return tag;
            }
        }
        return null;
    }

    public static WeexTag getWeexTag(String tagName) {
        if (tags == null) {
            prepare();
        }
        WeexTag common = null;
        WeexTag target = null;
        for (WeexTag tag : tags) {
            if (tag.tag.equals(tagName)) {
                target = tag;
            }
            if (tag.tag.equals("common")) {
                common = tag;
            }
        }
        if (common != null) {
            if (target != null) {
                return merge(target, common);
            } else {
                return common;
            }
        } else {
            if (target != null) {
                return target;
            }
        }
        return null;
    }

    private static WeexTag merge(WeexTag target, WeexTag common) {
        target.attrs.addAll(common.attrs);
        return target;
    }
}
