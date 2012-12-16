package rslauncher.handlers;

import org.eclipse.core.commands.ExecutionEvent
import org.eclipse.core.commands.ExecutionException
import org.eclipse.core.commands.AbstractHandler
import org.eclipse.ui.handlers.HandlerUtil
import org.eclipse.jface.dialogs.MessageDialog
import org.eclipse.jface.viewers.IStructuredSelection
import org.eclipse.jdt.core.ICompilationUnit
import org.eclipse.core.internal.resources.File
import rscore.dsl.entity.collection.RSCollection
import rscore.dsl.entity.RSClass
import rscore.interpreter.RSInterpreter
import rscore.interpreter.ScriptHelper
import java.io.BufferedReader
import java.io.InputStreamReader
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.Path
import rscore.dsl.entity.RSWorkspace
import rscore.dsl.entity.RSProject
import rscore.util.FileUtil
import org.eclipse.core.resources.IFile

class SampleHandler extends AbstractHandler {
	private val ScriptExtension = "rsl"

	/**
	 * ファイが属するプロジェクトを取得する
	 */
	private def getCurrentProject(file: IFile): RSProject = {
		val currentProjectName = file.getProject().getName()
		return RSWorkspace.project(currentProjectName)
	}
	
	// TODO テスト用に execute() を作る

	override def execute(event: ExecutionEvent): Object = {
		val selection = HandlerUtil.getCurrentSelection(event)
		if (selection.isInstanceOf[IStructuredSelection]) {
			val firstElement = selection.asInstanceOf[IStructuredSelection].getFirstElement()
			if (firstElement.isInstanceOf[File]) {
				// ファイルの内容を読み込んで，interpreter に渡す
				val selectedFile = firstElement.asInstanceOf[File]

				val file: IFile = RSWorkspace.root.getFileForLocation(selectedFile.getLocation().makeAbsolute())
				val ext = file.getFileExtension()

				val is = file.getContents()
				val sourceString = FileUtil.getContentsFromInputStream(is)
				val lines = sourceString.split("""\n""")

				val interpreter = RSInterpreter
				interpreter.initContainer()

				val builder = new StringBuilder

				lines.foreach(line => {
					// 特殊行の処理
					// ランタイム変数（%CURRENT_PROJECT など）
					if (line.indexOf(":=") != -1) {
						val vs = line.split(""":=""")
						assert(vs.length == 2)
						val lv = vs.first.replaceAll("""\s*""", "")
						val rv = vs.last.replaceAll("""\s*""", "")

						if (rv == """%CURRENT_PROJECT""") {
							val currentProject = getCurrentProject(file)
							interpreter.assignVariable(lv, currentProject)
						}
					} else {
						// 文字列置換などはここで
						builder.append(ScriptHelper.prepareScript(line) + "\n")
					}
				})

				val source = builder.toString()
				interpreter.execScript(source)
			}

		}

		val window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		MessageDialog.openInformation(
			window.getShell(),
			"Complete",
			"Script you specified is successfully executed!!");

		return null
	}
}

