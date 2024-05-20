# Setting up an application in a page

The Forum/Discussion application, which allows you to insert a discussion into the page, has the following display options:
- Discussion forum - simple discussion under the article
- `Message board` - Multi-topic discussion where the discussion is divided by groups into sub-topics that will contain individual contributions. Subtopics can also be added by the user.

## Discussion forum

A discussion forum is a type of simple discussion.

![](forum-list-forum.png)

The application setup consists of two tabs `Parametre aplikácie` a `Zoznam diskusií`

![](clasic-forum.png)

### Application parameters

The application parameters tab contains several additional parameters and constraints:
- `Zvoľte komponentu`, the choice between the Discussion Forum and `Message board` (choice of discussion type).
- `Usporiadať podľa času príspevku`, descending or ascending arrangement.
- `Spôsob zobrazenia textu stránky`
	- Embedded frame (iframe) - when displaying a complete listing of a discussion, the content of the original page is displayed in the frame.
	- Showing the perex - when you view the full discussion listing, only the perex of the original page will be displayed.
	- Not displayed at all - only the discussion listing will be displayed.
	- Normal - when the full discussion listing is displayed, the full text of the original page is also displayed
	- The entire forum, including texts - a complete listing of the entire discussion will be embedded in the page.
- `Stránkovať diskusiu`, an additional parameter appears when you allow paging the discussion `Počet príspevkov na stránke`, which determines how to paginate and its default value is 10.

### List of discussions

The discussion list tab is a nested page listing all discussions (both forum and message board). You can learn more about the discussion list here ['Discussion list'](forum-list.md).

## Message board

`Message board` is a type of multi-topic discussion. It can be embedded as an application in the page. The discussion is divided into groups (sections) and subtopics. For each subtopic, the number of posts added, the number of views, and the date the last post was added are recorded and displayed under the topic title.

So a visitor can create a new topic and then discussion posts are added to the topic. This creates a kind of tree structure of discussion posts.

![](forum-list-board.png)

The application setup consists of two tabs `Parametre aplikácie` a `Zoznam diskusií`

![](message-board.png)

### Application parameters

The application parameters tab contains several additional parameters:
- `Zvoľte komponentu`, selecting between Discussion forum and Message board (selecting the type of discussion).
- `Smer usporiadania`, descending or ascending arrangement.
- `Zoradienie tém` namely
	- According to the last post.
	- By the date the topic was created.
- `Počet príspevkov na stránke`, enter the number of posts that will appear on a single page. The default value is 10.
- `Počet zobrazených čísel stránok`, specifies the number of direct numeric links to pages from the paginated list. The default value is 10.
- `Zapnúť čas. limit na mazanie príspevkov`, turn it on if you want to allow posts to be deleted only until the time limit expires. Only the author has the right to delete a post.
- `Časový limit (min)`, give the numerical value in min. The preset value is 30 minutes.

### List of discussions

The discussion list tab is a nested page listing all discussions (both forum and message board). You can learn more about the discussion list here ['Discussion list'](forum-list.md).
