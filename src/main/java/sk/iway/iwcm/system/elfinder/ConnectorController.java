package sk.iway.iwcm.system.elfinder;

import java.io.IOException;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import cn.bluejoe.elfinder.controller.FsException;
import cn.bluejoe.elfinder.controller.executor.CommandExecutionContext;
import cn.bluejoe.elfinder.controller.executor.CommandExecutor;
import cn.bluejoe.elfinder.controller.executor.CommandExecutorFactory;
import cn.bluejoe.elfinder.controller.executor.DefaultCommandExecutorFactory;
import cn.bluejoe.elfinder.service.FsServiceFactory;

public class ConnectorController
{
	@Resource(name = "commandExecutorFactory")
	private CommandExecutorFactory _commandExecutorFactory = new DefaultCommandExecutorFactory();

	@Resource(name = "fsServiceFactory")
	private FsServiceFactory _fsServiceFactory;

	public void connector(HttpServletRequest request, final HttpServletResponse response) throws IOException
	{
		String cmd = request.getParameter("cmd");
		CommandExecutor ce = _commandExecutorFactory.get(cmd);

		if (ce == null)
		{
			throw new FsException(String.format("unknown command: %s", cmd));
		}

		_fsServiceFactory = new StaticFsServiceFactory();

		request.setAttribute("commandExecutorFactory", _commandExecutorFactory);
		try
		{
			final HttpServletRequest finalRequest = request;
			ce.execute(new CommandExecutionContext()
			{

				@Override
				public FsServiceFactory getFsServiceFactory()
				{
					return _fsServiceFactory;
				}

				@Override
				public HttpServletRequest getRequest()
				{
					return finalRequest;
				}

				@Override
				public HttpServletResponse getResponse()
				{
					return response;
				}

				@Override
				public ServletContext getServletContext()
				{
					return finalRequest.getSession().getServletContext();
				}

			});
		}
		catch (Exception e)
		{
			throw new FsException("unknown error", e);
		}
	}
}