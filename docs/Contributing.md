# Contributing to the Art of Illusion project

So, you would like to contribute to Art of Illusion? Great! Here's how.

## Contributing without programing:

There are several things you can do to help that do not require
any programing ability.

 * [Report a bug](https://sourceforge.net/p/aoi/bugs/)
 * [Propose a new feature](https://sourceforge.net/p/aoi/feature-requests/)
 * If you speak a language other than English, you can contribute,
review, or update a translation
 * Create a tutorial
 * Help update the website, www.artofillusion.org. We're currently in
the middle of re-designing the site. Please see the
[github project](https://github.com/ArtOfIllusion/AOI-website) for
details.

If you would like to help with any of these, please drop in to
the [discussion forums.](https://sourecforge.net/p/aoi/discussion)

------------------------------------------------------

## Contributing with code:

### For complete novices

Please take a few minutes to become familiar with the basics of git
and making a pull request on github. Github's documentation team has
provided an awesome
[getting started guide,](https://guides.github.com/activities/hello-world/)
and a special tour of
[forking,](https://guides.github.com/activities/forking) which you will
need to understand to contribute. Don't worry about the advanced stuff 
yet, we'll help you through it if you need a hand.

Next, follow our instructions for [building AOI](./Building.md) all
the way through to confirm that everything is working for you. 

### Choosing a project

If you came here with a specific bug or feature in mind, feel free to
work on it. 

If you are searching for a project, browse through our
[bug](https://sourceforge.net/p/aoi/bugs) and
[feature](https://sourceforge.net/p/aoi/feature-requests) lists.
If you are not sure how much experience or time might be required
for a specific project, ask on the
[forums.](https://sourceforge.net/p/aoi/discussion)

### How much should I put in a pull request?

Please put the changes related to *one* bug or feature in each pull
request.

 * If you change three files to fix one bug, that is one pull request.
 * If you change a file to fix a bug and then change the same file to
add a new feature, that is two pull requests.
 * If you are not sure about the scope of an idea, please ask.

### Code Style

A consistent formatting style throughout a codebase helps developers
to stay oriented and focused on the content. AOI generally follows the
following formating convention:

```java
public class Example
{
  public static String Braces = "Left, new line to start a block";
  public static int indentSpaces = 2;

  public void longParameterLists(boolean areStacked,
                                 String toPrevent,
				 int theLength,
				 String fromRunningInto,
				 int wrappingIssues)
  {
    if(youUseAConditionalWithAOneLiner)
       System.out.println("Just indent the statement to be executed!");

    for (Object obj: Collection group)
         obj.indent("One space past opening Parenthesis");
  }
}
```

This convention can flex if it gets too cumbersome, but should be a
good general guideline.

We're looking forward to seeing your pull requests!
